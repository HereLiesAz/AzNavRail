# Compose Destinations UX Analyzer

`.github/scripts/compose_destinations_ux.py` checks the behavior around generated destinations. KSP can prove a route exists. It cannot prove the route is not a furnished oubliette.

## Run it

~~~shell
python .github/scripts/compose_destinations_ux.py SampleApp/src/main --root .
~~~

Errors return status `1`; warnings are reported without blocking by default. Tighten or disable the gate with `--fail-on warning` or `--fail-on none`.

## Reports

| Format | Command option | Purpose |
| --- | --- | --- |
| Text | `--format text` | Local terminal diagnostics. This is the default. |
| JSON | `--format json` | Structured output for custom tooling. |
| GitHub | `--format github` | Native workflow warning/error annotations attached to Kotlin lines. |
| SARIF | `--format sarif` | GitHub code-scanning results with rule metadata, severity, file, and line. |

Use `--output PATH` to write any format to disk. Use `--summary "$GITHUB_STEP_SUMMARY"` to append error/warning totals and a Markdown finding list to a workflow summary.

## Rules

### `orphaned-error-state`

A `@Destination` accepts a sealed state containing an error-like variant (`Error`, `Failure`, `Timeout`, `Denied`, `Unavailable`, `Invalid`, or `Expired`) but its body has neither:

- an outgoing `navigate`, `navigateTo`, or `direction` call; nor
- a retry, back, skip, support, cancel, close, or exit action.

The state may be terminal. The user may not be.

### `incomplete-decision-matrix`

A destination branches with `when (enumParameter)`, but one or more declared enum values have no explicit branch. An `else` branch does not silence the diagnostic: a new enum value disappearing into generic behavior is the bug wearing a raincoat.

### `circular-trap-state`

Two or more destinations form a closed navigation cycle, or one destination routes to itself, with no outgoing route and no escape action in the component. The analyzer reports one diagnostic for the strongly connected component.

### `unreachable-destination`

**Severity: warning.** When at least one destination declares `start = true`, every other destination must be reachable through a parsed navigation event. Dynamic or externally initiated routes may be legitimate; the warning demands that the exception be seen rather than buried with the rest of the bones.

## Source conventions

- Destination entry points use `@Destination` and `@Composable` on the function.
- View-state models use a sealed interface or sealed class with nested or sibling object/class variants.
- Decision inputs use enum classes and explicit `when` branches.
- Navigation events call `navigate`, `navigateTo`, or `direction` with a generated `*Destination` symbol.
- Escape actions contain an explicit semantic operation such as `retry`, `popBackStack`, `navigateUp`, `skip`, or `support`.

The analyzer is intentionally conservative and source-only. It does not execute Kotlin, infer callbacks hidden behind arbitrary names, or pretend an `else` branch documents a product decision.

## GitHub workflow

The `ux-analysis` job in `.github/workflows/android-sample-build.yml` runs on pushes and pull requests before the Android build:

1. Run the analyzer unit tests.
2. Generate a SARIF report without failing early.
3. Emit native annotations and append the job summary.
4. Upload SARIF to GitHub code scanning for same-repository branches.
5. Preserve SARIF as a workflow artifact, including on failure.
6. Fail the required job when an error remains. Warnings remain visible but non-blocking.

SARIF upload is skipped for forked pull requests because their read-only token cannot write security events. The artifact, annotations, summary, and enforcement still run. Security gets a boundary; bureaucracy gets a checkbox; the user gets neither excuse.

## Tests

~~~shell
python .github/scripts/test_compose_destinations_ux.py
~~~

The test suite fixes each expected value independently: trapped and recovered error states, a three-value enum with exactly one omitted branch, closed and escapable loops, an unreachable warning, SARIF location/severity mapping, workflow-command escaping, and Markdown severity totals.
