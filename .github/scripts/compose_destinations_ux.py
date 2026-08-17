#!/usr/bin/env python3
"""Find UX dead ends in Kotlin sources using Compose Destinations.

The analyzer is deliberately source-only: it runs before KSP and needs no Android
toolchain. It reports orphaned error states, enum decisions without a branch for
every value, closed navigation cycles without an escape action, and unreachable
destinations. Reports can be emitted as terminal text, JSON, GitHub annotations,
or SARIF for code scanning.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


DESTINATION = re.compile(
    r"@Destination(?:<[^>]+>)?(?:\([^\n)]*\))?\s*"
    r"(?:@\w+(?:\([^\n)]*\))?\s*)*@Composable\s+fun\s+(\w+)\s*\(",
    re.MULTILINE,
)
ENUM = re.compile(r"\benum\s+class\s+(\w+)[^{]*\{")
SEALED = re.compile(r"\bsealed\s+(?:interface|class)\s+(\w+)")
STATE_CHILD = re.compile(r"\b(?:data\s+)?(?:object|class)\s+(\w+)")
PARAMETER = re.compile(r"(?:val\s+|var\s+)?(\w+)\s*:\s*([\w.<>?]+)")
NAVIGATION = re.compile(
    r"(?:navigate|direction|navigateTo)\s*\(\s*(?:directions\.)?"
    r"(\w+?)(?:Destination)?(?=\s*[\s(,)])",
    re.MULTILINE,
)
ERROR_NAME = re.compile(r"(?:Error|Failure|Timeout|Denied|Unavailable|Invalid|Expired)", re.IGNORECASE)
_ESCAPE_WORD = r"(?:skip|support|cancel|close|back|popBackStack|navigateUp|retry|exit)"
# An escape must look like an actual invocation or callback reference (`retry()`, `nav::navigateUp`),
# not merely the word appearing anywhere -- a label like `Text("Retry")` is not an escape action.
ESCAPE = re.compile(rf"\b{_ESCAPE_WORD}\b\s*\(|::{_ESCAPE_WORD}\b", re.IGNORECASE)
PACKAGE = re.compile(r"^\s*package\s+([\w.]+)", re.MULTILINE)

RULES = {
    "orphaned-error-state": ("Orphaned error state", "error"),
    "incomplete-decision-matrix": ("Incomplete decision matrix", "error"),
    "circular-trap-state": ("Circular trap state", "error"),
    "unreachable-destination": ("Unreachable destination", "warning"),
}
SEVERITY_RANK = {"none": 3, "warning": 1, "error": 2}


@dataclass(frozen=True)
class Finding:
    rule: str
    severity: str
    file: str
    line: int
    message: str

    def as_dict(self) -> dict[str, str | int]:
        return self.__dict__.copy()


@dataclass
class Destination:
    name: str
    file: Path
    line: int
    parameters: dict[str, str]
    body: str
    start: bool = False
    edges: set[str] = field(default_factory=set)


def _matching(source: str, start: int, opening: str, closing: str) -> int:
    """Return the matching delimiter index, ignoring strings and comments."""
    depth = 0
    quote: str | None = None
    escaped = False
    index = start
    while index < len(source):
        char = source[index]
        pair = source[index:index + 2]
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
        elif pair == "//":
            newline = source.find("\n", index + 2)
            index = len(source) if newline == -1 else newline
            continue
        elif pair == "/*":
            end = source.find("*/", index + 2)
            index = len(source) if end == -1 else end + 2
            continue
        elif char in {'"', "'"}:
            quote = char
        elif char == opening:
            depth += 1
        elif char == closing:
            depth -= 1
            if depth == 0:
                return index
        index += 1
    return -1


def _block(source: str, opening_index: int, opening: str = "{", closing: str = "}") -> str:
    end = _matching(source, opening_index, opening, closing)
    return source[opening_index + 1:end] if end >= 0 else ""


def _strip_noncode(source: str) -> str:
    """Blank string/char literals and comments so keyword regexes never match inside them."""
    result: list[str] = []
    quote: str | None = None
    escaped = False
    index = 0
    length = len(source)
    while index < length:
        char = source[index]
        pair = source[index:index + 2]
        if quote:
            result.append("\n" if char == "\n" else " ")
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
            index += 1
            continue
        if pair == "//":
            newline = source.find("\n", index + 2)
            end = length if newline == -1 else newline
            result.append(" " * (end - index))
            index = end
            continue
        if pair == "/*":
            end = source.find("*/", index + 2)
            end = length if end == -1 else end + 2
            result.append("".join("\n" if ch == "\n" else " " for ch in source[index:end]))
            index = end
            continue
        if char in {'"', "'"}:
            quote = char
            result.append(" ")
            index += 1
            continue
        result.append(char)
        index += 1
    return "".join(result)


def _has_escape(text: str) -> bool:
    return bool(ESCAPE.search(_strip_noncode(text)))


def _branch_body(body: str, start: int) -> str:
    """Return a `when` branch's code, starting just after its `->`.

    Stops at the next sibling branch (a `->` at the same nesting depth) or at the point the
    branch's own brackets close back past where it started, whichever comes first.
    """
    depth = 0
    quote: str | None = None
    escaped = False
    index = start
    length = len(body)
    while index < length:
        char = body[index]
        pair = body[index:index + 2]
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
            index += 1
            continue
        if pair == "//":
            newline = body.find("\n", index + 2)
            index = length if newline == -1 else newline
            continue
        if pair == "/*":
            end = body.find("*/", index + 2)
            index = length if end == -1 else end + 2
            continue
        if char in {'"', "'"}:
            quote = char
        elif char in "({[":
            depth += 1
        elif char in ")}]":
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and pair == "->" and index != start:
            break
        index += 1
    return body[start:index]


def _expression_body(source: str, start: int) -> str:
    """Return an expression-bodied function's code, starting just after its `=`.

    An expression body (`fun Home() = HomeScreen()`) has no braces to bound it, so instead of
    borrowing the next `{` found anywhere later in the file (which either drops the destination
    entirely or steals an unrelated function's block), scan forward and stop at the first
    unmatched closing bracket or a newline/`;` reached at zero nesting depth -- the point the
    statement actually ends.
    """
    depth = 0
    quote: str | None = None
    escaped = False
    index = start
    length = len(source)
    while index < length:
        char = source[index]
        pair = source[index:index + 2]
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
            index += 1
            continue
        if pair == "//":
            newline = source.find("\n", index + 2)
            index = length if newline == -1 else newline
            continue
        if pair == "/*":
            end = source.find("*/", index + 2)
            index = length if end == -1 else end + 2
            continue
        if char in {'"', "'"}:
            quote = char
        elif char in "({[":
            depth += 1
        elif char in ")}]":
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and char in {"\n", ";"}:
            break
        index += 1
    return source[start:index]


def _type_name(raw: str) -> str:
    # Keep any qualifier the source actually wrote (`checkout.State`, not just `State`) -- collapsing
    # it away is what lets same-named sealed/enum types from different packages collide below.
    return raw.rstrip("?").split("<", 1)[0]


def _enum_values(declarations: str) -> set[str]:
    """Return only the leading identifier of each top-level enum entry.

    A blind `findall` over the whole declarations text also matches constructor-argument
    references such as `Color.RED` in `YES(Color.RED), NO(Color.BLUE)`; walk the text instead so
    only the identifier immediately after the start of the text or a top-level comma counts.
    """
    values: set[str] = set()
    depth = 0
    index = 0
    length = len(declarations)
    expect_entry = True
    while index < length:
        char = declarations[index]
        if char in "([{":
            depth += 1
        elif char in ")]}":
            depth = max(0, depth - 1)
        elif depth == 0 and char == ",":
            expect_entry = True
            index += 1
            continue
        elif depth == 0 and expect_entry and not char.isspace():
            match = re.match(r"[A-Z][A-Z0-9_]*\b", declarations[index:])
            if match and match.group(0) != "TODO":
                values.add(match.group(0))
            expect_entry = False
            if match:
                index += match.end()
                continue
        index += 1
    return values


def _index_types(table: dict[str, set[str]], declarations: list[tuple[str, str | None, set[str]]]) -> None:
    """Key each declaration by its package-qualified name, plus a bare-name alias when that bare
    name is unambiguous across the analyzed sources.

    Two distinct sealed/enum types sharing a bare name (e.g. `checkout.State` and `profile.State`)
    must never be pooled under one shared bucket -- so the unqualified alias is only registered
    when exactly one declaration claims that bare name.
    """
    bare_counts: dict[str, int] = {}
    for name, _package, _values in declarations:
        bare_counts[name] = bare_counts.get(name, 0) + 1
    for name, package, values in declarations:
        table[f"{package}.{name}" if package else name] = values
        if bare_counts[name] == 1:
            table[name] = values


def parse_sources(paths: Iterable[Path]) -> tuple[dict[str, set[str]], dict[str, set[str]], dict[str, Destination]]:
    """Parse enum values, sealed-state variants, and destination functions."""
    destinations: dict[str, Destination] = {}
    sources: list[tuple[Path, str, str | None]] = []
    enum_declarations: list[tuple[str, str | None, set[str]]] = []
    state_declarations: list[tuple[str, str | None, set[str]]] = []
    for path in paths:
        source = path.read_text(encoding="utf-8")
        package_match = PACKAGE.search(source)
        package = package_match.group(1) if package_match else None
        sources.append((path, source, package))
        for match in ENUM.finditer(source):
            body = _block(source, source.find("{", match.start()))
            declarations = body.split(";", 1)[0]
            enum_declarations.append((match.group(1), package, _enum_values(declarations)))
        for match in SEALED.finditer(source):
            line_end = source.find("\n", match.end())
            body_open = source.find("{", match.end(), len(source) if line_end < 0 else line_end)
            body = _block(source, body_open) if body_open >= 0 else ""
            state_declarations.append((match.group(1), package, {child.group(1) for child in STATE_CHILD.finditer(body)}))

    # Sealed implementations are often siblings rather than lexically nested. Resolve these only
    # after collecting every sealed type, scoped to sources sharing that type's package -- so two
    # same-named sealed types declared in different packages never pool each other's variants.
    for name, package, variants in state_declarations:
        implementation = re.compile(
            rf"\b(?:data\s+)?(?:object|class)\s+(\w+)[^\n{{}}]*:\s*[^\n{{}}]*\b{re.escape(name)}\b"
        )
        for _, source, source_package in sources:
            if source_package != package:
                continue
            variants.update(match.group(1) for match in implementation.finditer(source))

    enums: dict[str, set[str]] = {}
    states: dict[str, set[str]] = {}
    _index_types(enums, enum_declarations)
    _index_types(states, state_declarations)

    for path, source, _package in sources:
        for match in DESTINATION.finditer(source):
            # `match` already consumed the function's own opening paren (the pattern ends in a
            # literal `\(`) -- searching the source again for the next `(` from match.start()
            # would instead find an argumented `@Destination(...)`'s own paren when present.
            params_open = match.end() - 1
            params_close = _matching(source, params_open, "(", ")")
            if params_close < 0:
                continue
            after_params = source[params_close + 1:]
            return_type = re.match(r"\s*:\s*[\w.<>?]+", after_params)
            skip = return_type.end() if return_type else 0
            stripped = after_params[skip:].lstrip()
            skip += len(after_params[skip:]) - len(stripped)
            body_start = params_close + 1 + skip
            if stripped.startswith("{"):
                body = _block(source, body_start)
            elif stripped.startswith("="):
                body = _expression_body(source, body_start + 1)
            else:
                # No block or expression body found (e.g. an `expect`/abstract declaration) --
                # nothing to analyze.
                continue
            parameters = {name: _type_name(kind) for name, kind in PARAMETER.findall(source[params_open + 1:params_close])}
            destination = Destination(
                name=match.group(1),
                file=path,
                line=source.count("\n", 0, match.start()) + 1,
                parameters=parameters,
                body=body,
                start=bool(re.search(r"\bstart\s*=\s*true\b", match.group(0))),
            )
            destination.edges = {edge for edge in NAVIGATION.findall(body)}
            destinations[destination.name] = destination
    return enums, states, destinations


def analyze(paths: Iterable[Path], root: Path) -> list[Finding]:
    """Return deterministic diagnostics for supported UX flaws."""
    enums, states, destinations = parse_sources(paths)
    findings: list[Finding] = []
    for destination in destinations.values():
        relative = str(destination.file.relative_to(root))
        error_variants = [
            (kind, variant)
            for kind in destination.parameters.values()
            for variant in states.get(kind, set())
            if ERROR_NAME.search(variant)
        ]
        orphaned = []
        for kind, variant in error_variants:
            # Scope the escape/exit check to the specific `when` branch handling this error state
            # when one exists, so an escape action anywhere else in the function (including an
            # unrelated success branch) can't mask a genuinely trapped error branch.
            arrow = re.search(rf"\b(?:{re.escape(kind)}\.)?{re.escape(variant)}\b\s*->", destination.body)
            if arrow:
                branch = _branch_body(destination.body, arrow.end())
                has_edge = bool(NAVIGATION.search(branch))
            else:
                branch = destination.body
                has_edge = bool(destination.edges)
            if not has_edge and not _has_escape(branch):
                orphaned.append(f"{kind}.{variant}")
        if orphaned:
            findings.append(Finding(
                "orphaned-error-state", "error", relative, destination.line,
                f"{destination.name} accepts {', '.join(sorted(orphaned))} but exposes no retry, back, escape, or outgoing direction.",
            ))
        for parameter, kind in destination.parameters.items():
            values = enums.get(kind)
            if not values or not re.search(rf"\bwhen\s*\(\s*{re.escape(parameter)}\s*\)", destination.body):
                continue
            handled = {
                value for value in values
                if re.search(
                    rf"\b(?:{re.escape(kind)}\.)?{re.escape(value)}\b(?:\s*,\s*(?:{re.escape(kind)}\.)?\w+\b)*\s*->",
                    destination.body,
                )
            }
            missing = sorted(values - handled)
            if missing:
                findings.append(Finding(
                    "incomplete-decision-matrix", "error", relative, destination.line,
                    f"{destination.name} omits {kind} cases: {', '.join(missing)}.",
                ))

    graph = {name: {edge for edge in item.edges if edge in destinations} for name, item in destinations.items()}
    full_edges = {name: item.edges for name, item in destinations.items()}
    for component in _closed_cycles(graph, full_edges):
        members = [destinations[name] for name in sorted(component)]
        if any(_has_escape(member.body) for member in members):
            continue
        first = members[0]
        findings.append(Finding(
            "circular-trap-state", "error", str(first.file.relative_to(root)), first.line,
            f"Closed destination cycle has no skip, support, cancel, back, or exit: {' -> '.join(member.name for member in members)}.",
        ))

    starts = {name for name, destination in destinations.items() if destination.start}
    if starts:
        reachable = set(starts)
        pending = list(starts)
        while pending:
            for target in graph[pending.pop()]:
                if target not in reachable:
                    reachable.add(target)
                    pending.append(target)
        for name in sorted(destinations.keys() - reachable):
            destination = destinations[name]
            findings.append(Finding(
                "unreachable-destination", "warning", str(destination.file.relative_to(root)), destination.line,
                f"{name} cannot be reached from any destination declared with start = true.",
            ))
    return sorted(findings, key=lambda item: (item.file, item.line, item.rule))


def _closed_cycles(graph: dict[str, set[str]], full_edges: dict[str, set[str]] | None = None) -> list[set[str]]:
    """Return strongly connected components that are cycles with no external edge.

    `graph` is used to walk reachability, so it must only contain edges to other known
    destinations (Tarjan's algorithm indexes into it by target). `full_edges` -- the destination's
    *unfiltered* edge set, including targets outside the analyzed source set entirely -- is used
    for the closure check instead, so an edge to a destination declared in another module the
    caller didn't pass in still counts as a real way out rather than being silently dropped.
    """
    edges_for_closure = full_edges if full_edges is not None else graph
    index = 0
    stack: list[str] = []
    indices: dict[str, int] = {}
    low: dict[str, int] = {}
    on_stack: set[str] = set()
    result: list[set[str]] = []

    def visit(node: str) -> None:
        nonlocal index
        indices[node] = low[node] = index
        index += 1
        stack.append(node)
        on_stack.add(node)
        for target in graph[node]:
            if target not in indices:
                visit(target)
                low[node] = min(low[node], low[target])
            elif target in on_stack:
                low[node] = min(low[node], indices[target])
        if low[node] != indices[node]:
            return
        component: set[str] = set()
        while stack:
            member = stack.pop()
            on_stack.remove(member)
            component.add(member)
            if member == node:
                break
        is_cycle = len(component) > 1 or any(member in graph[member] for member in component)
        is_closed = not any(target not in component for member in component for target in edges_for_closure[member])
        if is_cycle and is_closed:
            result.append(component)

    for node in graph:
        if node not in indices:
            visit(node)
    return result


def has_any_destination(paths: Iterable[Path]) -> bool:
    """Return whether any of the given files declares an `@Destination` composable.

    A run over paths with none is a no-op that trivially "passes" -- distinguishing that case
    lets callers refuse to treat an empty analysis as a clean report.
    """
    return any(DESTINATION.search(path.read_text(encoding="utf-8")) for path in paths)


def kotlin_files(inputs: list[Path]) -> list[Path]:
    """Expand files and directories into a stable Kotlin source list."""
    files: set[Path] = set()
    for item in inputs:
        if item.is_file() and item.suffix == ".kt":
            files.add(item.resolve())
        elif item.is_dir():
            files.update(path.resolve() for path in item.rglob("*.kt") if "build" not in path.parts)
    return sorted(files)


def sarif(findings: list[Finding]) -> dict[str, object]:
    """Build a SARIF 2.1.0 report accepted by GitHub code scanning."""
    rules = [{
        "id": rule,
        "name": rule.replace("-", "_"),
        "shortDescription": {"text": title},
        "defaultConfiguration": {"level": severity},
    } for rule, (title, severity) in RULES.items()]
    results = [{
        "ruleId": finding.rule,
        "level": finding.severity,
        "message": {"text": finding.message},
        "locations": [{"physicalLocation": {
            "artifactLocation": {"uri": finding.file},
            "region": {"startLine": finding.line},
        }}],
    } for finding in findings]
    return {
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "version": "2.1.0",
        "runs": [{
            "tool": {"driver": {
                "name": "Compose Destinations UX Analyzer",
                "informationUri": "https://github.com/HereLiesAz/AzNavRail/blob/master/docs/COMPOSE_DESTINATIONS_UX_ANALYZER.md",
                "rules": rules,
            }},
            "results": results,
        }],
    }


def github_annotations(findings: list[Finding]) -> str:
    """Render workflow-command annotations beside the offending Kotlin lines."""
    def escape(value: str) -> str:
        return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")

    return "\n".join(
        f"::{finding.severity} file={escape(finding.file)},line={finding.line},title={escape(RULES[finding.rule][0])}::{escape(finding.message)}"
        for finding in findings
    )


def markdown_summary(findings: list[Finding], found_any_destination: bool = True) -> str:
    """Render a compact GitHub step summary with severity totals and findings."""
    errors = sum(finding.severity == "error" for finding in findings)
    warnings = sum(finding.severity == "warning" for finding in findings)
    lines = [
        "## Compose Destinations UX report",
        "",
        f"**{errors} errors · {warnings} warnings**",
        "",
    ]
    if not found_any_destination:
        lines.append(
            "_No `@Destination`-annotated composables were found in the analyzed paths -- "
            "there was nothing for this report to check._"
        )
        lines.append("")
    if not findings:
        lines.append("No UX graph defects found.")
    else:
        lines.extend(
            f"- **{finding.severity.upper()}** `{finding.rule}` — `{finding.file}:{finding.line}` — {finding.message}"
            for finding in findings
        )
    return "\n".join(lines) + "\n"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="+", type=Path, help="Kotlin files or source directories")
    parser.add_argument("--format", choices=("text", "json", "github", "sarif"), default="text")
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="Root used in diagnostic paths")
    parser.add_argument("--output", type=Path, help="Write the report to a file instead of stdout")
    parser.add_argument("--summary", type=Path, help="Append a Markdown report to a GitHub step-summary file")
    parser.add_argument(
        "--fail-on", choices=("error", "warning", "none"), default="error",
        help="Lowest severity that returns status 1; default: error",
    )
    parser.add_argument(
        "--require-destinations", action="store_true",
        help=(
            "Also return status 1 if no @Destination-annotated composables were found in the "
            "given paths -- otherwise an empty analysis silently reports as clean."
        ),
    )
    args = parser.parse_args(argv)
    root = args.root.resolve()
    files = kotlin_files(args.paths)
    found_any_destination = has_any_destination(files)
    if not found_any_destination:
        sys.stderr.write(
            "compose_destinations_ux: no @Destination-annotated composables were found in the "
            "given paths -- there is nothing to analyze.\n"
        )
    findings = analyze(files, root)
    if args.format == "json":
        report = json.dumps([finding.as_dict() for finding in findings], indent=2) + "\n"
    elif args.format == "sarif":
        report = json.dumps(sarif(findings), indent=2) + "\n"
    elif args.format == "github":
        report = github_annotations(findings) + ("\n" if findings else "")
    else:
        report = "".join(
            f"{finding.file}:{finding.line}: {finding.severity}: {finding.message} [{finding.rule}]\n"
            for finding in findings
        )
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(report, encoding="utf-8")
    else:
        print(report, end="")
    if args.summary:
        args.summary.parent.mkdir(parents=True, exist_ok=True)
        with args.summary.open("a", encoding="utf-8") as summary_file:
            summary_file.write(markdown_summary(findings, found_any_destination))
    threshold = SEVERITY_RANK[args.fail_on]
    failed = any(SEVERITY_RANK[finding.severity] >= threshold for finding in findings)
    if args.require_destinations and not found_any_destination:
        failed = True
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
