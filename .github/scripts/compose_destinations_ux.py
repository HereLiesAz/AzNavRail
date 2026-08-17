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
ESCAPE = re.compile(r"\b(?:skip|support|cancel|close|back|popBackStack|navigateUp|retry|exit)\b", re.IGNORECASE)

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


def _type_name(raw: str) -> str:
    return raw.rstrip("?").split("<", 1)[0].rsplit(".", 1)[-1]


def parse_sources(paths: Iterable[Path]) -> tuple[dict[str, set[str]], dict[str, set[str]], dict[str, Destination]]:
    """Parse enum values, sealed-state variants, and destination functions."""
    enums: dict[str, set[str]] = {}
    states: dict[str, set[str]] = {}
    destinations: dict[str, Destination] = {}
    sources: list[tuple[Path, str]] = []
    for path in paths:
        source = path.read_text(encoding="utf-8")
        sources.append((path, source))
        for match in ENUM.finditer(source):
            body = _block(source, source.find("{", match.start()))
            declarations = body.split(";", 1)[0]
            enums[match.group(1)] = {
                token for token in re.findall(r"\b[A-Z][A-Z0-9_]*\b", declarations)
                if token not in {"TODO"}
            }
        for match in SEALED.finditer(source):
            line_end = source.find("\n", match.end())
            body_open = source.find("{", match.end(), len(source) if line_end < 0 else line_end)
            body = _block(source, body_open) if body_open >= 0 else ""
            states[match.group(1)] = {child.group(1) for child in STATE_CHILD.finditer(body)}

    # Sealed implementations are often siblings rather than lexically nested. Resolve these only
    # after collecting every sealed type so declarations can live in separate source files.
    for _, source in sources:
        for state in states:
            implementation = re.compile(
                rf"\b(?:data\s+)?(?:object|class)\s+(\w+)[^\n{{}}]*:\s*[^\n{{}}]*\b{re.escape(state)}\b"
            )
            states[state].update(match.group(1) for match in implementation.finditer(source))

    for path, source in sources:
        for match in DESTINATION.finditer(source):
            params_open = source.find("(", match.start())
            params_close = _matching(source, params_open, "(", ")")
            body_open = source.find("{", params_close)
            if params_close < 0 or body_open < 0:
                continue
            body = _block(source, body_open)
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
        error_states = sorted(
            f"{kind}.{variant}"
            for kind in destination.parameters.values()
            for variant in states.get(kind, set())
            if ERROR_NAME.search(variant)
        )
        if error_states and not destination.edges and not ESCAPE.search(destination.body):
            findings.append(Finding(
                "orphaned-error-state", "error", relative, destination.line,
                f"{destination.name} accepts {', '.join(error_states)} but exposes no retry, back, escape, or outgoing direction.",
            ))
        for parameter, kind in destination.parameters.items():
            values = enums.get(kind)
            if not values or not re.search(rf"\bwhen\s*\(\s*{re.escape(parameter)}\s*\)", destination.body):
                continue
            handled = {value for value in values if re.search(rf"\b(?:{re.escape(kind)}\.)?{re.escape(value)}\b\s*->", destination.body)}
            missing = sorted(values - handled)
            if missing:
                findings.append(Finding(
                    "incomplete-decision-matrix", "error", relative, destination.line,
                    f"{destination.name} omits {kind} cases: {', '.join(missing)}.",
                ))

    graph = {name: {edge for edge in item.edges if edge in destinations} for name, item in destinations.items()}
    for component in _closed_cycles(graph):
        members = [destinations[name] for name in sorted(component)]
        if any(ESCAPE.search(member.body) for member in members):
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


def _closed_cycles(graph: dict[str, set[str]]) -> list[set[str]]:
    """Return strongly connected components that are cycles with no external edge."""
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
        is_closed = not any(target not in component for member in component for target in graph[member])
        if is_cycle and is_closed:
            result.append(component)

    for node in graph:
        if node not in indices:
            visit(node)
    return result


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


def markdown_summary(findings: list[Finding]) -> str:
    """Render a compact GitHub step summary with severity totals and findings."""
    errors = sum(finding.severity == "error" for finding in findings)
    warnings = sum(finding.severity == "warning" for finding in findings)
    lines = [
        "## Compose Destinations UX report",
        "",
        f"**{errors} errors · {warnings} warnings**",
        "",
    ]
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
    args = parser.parse_args(argv)
    root = args.root.resolve()
    findings = analyze(kotlin_files(args.paths), root)
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
            summary_file.write(markdown_summary(findings))
    threshold = SEVERITY_RANK[args.fail_on]
    return int(any(SEVERITY_RANK[finding.severity] >= threshold for finding in findings))


if __name__ == "__main__":
    sys.exit(main())
