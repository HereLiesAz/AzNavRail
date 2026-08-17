#!/usr/bin/env python3
"""Tests for the Compose Destinations UX analyzer."""

from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("compose_destinations_ux.py")
SPEC = importlib.util.spec_from_file_location("compose_destinations_ux", SCRIPT)
assert SPEC and SPEC.loader
ux = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = ux
SPEC.loader.exec_module(ux)


class AnalyzerTest(unittest.TestCase):
    def analyze(self, source: str):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "Flow.kt"
            path.write_text(source, encoding="utf-8")
            return ux.analyze([path], root)

    def analyze_files(self, sources: dict[str, str]):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            paths = []
            for name, source in sources.items():
                path = root / name
                path.write_text(source, encoding="utf-8")
                paths.append(path)
            return ux.analyze(paths, root)

    def test_reports_orphaned_error_state(self):
        findings = self.analyze("""
            sealed interface AuthState
            data object Ready : AuthState
            data object NetworkTimeout : AuthState
            @Destination @Composable fun AuthScreen(state: AuthState) { Text(state.toString()) }
        """)
        self.assertEqual(["orphaned-error-state"], [item.rule for item in findings])

    def test_retry_is_an_escape_from_error_state(self):
        findings = self.analyze("""
            sealed interface AuthState { data object NetworkTimeout : AuthState }
            @Destination @Composable fun AuthScreen(state: AuthState) { Button(onClick = { retry() }) {} }
        """)
        self.assertEqual([], findings)

    def test_reports_missing_enum_branch(self):
        findings = self.analyze("""
            enum class UserType { GUEST, PREMIUM, STANDARD }
            @Destination @Composable fun Router(userType: UserType, navigator: DestinationsNavigator) {
                when (userType) {
                    UserType.GUEST -> navigator.navigate(GuestDestination)
                    UserType.PREMIUM -> navigator.navigate(PremiumDestination)
                }
            }
        """)
        self.assertEqual("incomplete-decision-matrix", findings[0].rule)
        self.assertIn("STANDARD", findings[0].message)

    def test_reports_closed_cycle_without_escape(self):
        findings = self.analyze("""
            @Destination @Composable fun Details(navigator: DestinationsNavigator) { navigator.navigate(ValidateDestination) }
            @Destination @Composable fun Validate(navigator: DestinationsNavigator) { navigator.navigate(DetailsDestination) }
        """)
        self.assertEqual(["circular-trap-state"], [item.rule for item in findings])

    def test_external_exit_prevents_cycle_finding(self):
        findings = self.analyze("""
            @Destination @Composable fun Details(navigator: DestinationsNavigator) {
                navigator.navigate(ValidateDestination); navigator.navigate(SupportDestination)
            }
            @Destination @Composable fun Validate(navigator: DestinationsNavigator) { navigator.navigate(DetailsDestination) }
            @Destination @Composable fun Support() { Text("Help") }
        """)
        self.assertEqual([], findings)

    def test_reports_unreachable_destination_as_warning(self):
        findings = self.analyze("""
            @Destination(start = true) @Composable fun Home(navigator: DestinationsNavigator) {
                navigator.navigate(ProfileDestination)
            }
            @Destination @Composable fun Profile() { Text("Profile") }
            @Destination @Composable fun Forgotten() { Text("Nobody calls") }
        """)
        self.assertEqual(["unreachable-destination"], [item.rule for item in findings])
        self.assertEqual("warning", findings[0].severity)

    def test_sarif_maps_rule_severity_and_location(self):
        finding = ux.Finding("orphaned-error-state", "error", "Flow.kt", 12, "No escape.")
        report = ux.sarif([finding])
        self.assertEqual("2.1.0", report["version"])
        result = report["runs"][0]["results"][0]
        self.assertEqual("orphaned-error-state", result["ruleId"])
        self.assertEqual(12, result["locations"][0]["physicalLocation"]["region"]["startLine"])
        json.dumps(report)

    def test_github_annotations_escape_workflow_commands(self):
        finding = ux.Finding("unreachable-destination", "warning", "Flow.kt", 7, "Lost%\nagain")
        annotation = ux.github_annotations([finding])
        self.assertEqual(
            "::warning file=Flow.kt,line=7,title=Unreachable destination::Lost%25%0Aagain",
            annotation,
        )

    def test_markdown_summary_counts_severities(self):
        findings = [
            ux.Finding("orphaned-error-state", "error", "A.kt", 1, "Dead."),
            ux.Finding("unreachable-destination", "warning", "B.kt", 2, "Lost."),
        ]
        summary = ux.markdown_summary(findings)
        self.assertIn("**1 errors · 1 warnings**", summary)
        self.assertIn("`B.kt:2`", summary)

    def test_destination_with_annotation_arguments_still_parses_parameters(self):
        # An argumented @Destination(...) has its own opening paren before the function's; the
        # parameter search must land on the function's, not the annotation's.
        findings = self.analyze("""
            sealed interface AuthState
            data object NetworkTimeout : AuthState
            @Destination<RootGraph>(route = "auth")
            @Composable fun AuthScreen(state: AuthState) { Text(state.toString()) }
        """)
        self.assertEqual(["orphaned-error-state"], [item.rule for item in findings])
        self.assertIn("AuthState.NetworkTimeout", findings[0].message)

    def test_escape_word_inside_string_literal_is_not_an_escape(self):
        findings = self.analyze("""
            sealed interface AuthState { data object NetworkTimeout : AuthState }
            @Destination @Composable fun AuthScreen(state: AuthState) { Text("Retry") }
        """)
        self.assertEqual(["orphaned-error-state"], [item.rule for item in findings])

    def test_has_any_destination(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            empty = root / "Empty.kt"
            empty.write_text("class NotADestination\n", encoding="utf-8")
            self.assertFalse(ux.has_any_destination([empty]))
            populated = root / "Populated.kt"
            populated.write_text(
                '@Destination @Composable fun Home() { Text("Home") }\n', encoding="utf-8"
            )
            self.assertTrue(ux.has_any_destination([empty, populated]))

    def test_require_destinations_flag_fails_on_empty_analysis(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "Empty.kt"
            path.write_text("class NotADestination\n", encoding="utf-8")
            with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                lenient_status = ux.main([str(path), "--root", str(root)])
                strict_status = ux.main([str(path), "--root", str(root), "--require-destinations"])
        self.assertEqual(0, lenient_status)
        self.assertEqual(1, strict_status)

    def test_expression_bodied_destination_is_parsed(self):
        # A `= expr` body has no braces of its own; the parser must not borrow the next
        # function's `{ ... }` block as if it belonged to this one.
        findings = self.analyze("""
            sealed interface AuthState { data object NetworkTimeout : AuthState }
            @Destination @Composable fun AuthScreen(state: AuthState) = Text(state.toString())
            @Destination @Composable fun NextScreen(navigator: DestinationsNavigator) {
                navigator.navigate(HomeDestination)
            }
        """)
        self.assertEqual(["orphaned-error-state"], [item.rule for item in findings])
        self.assertTrue(findings[0].message.startswith("AuthScreen"))

    def test_escape_in_unrelated_branch_does_not_mask_orphaned_error_branch(self):
        findings = self.analyze("""
            sealed interface AuthState
            data object Ready : AuthState
            data object FatalError : AuthState
            @Destination @Composable fun AuthScreen(state: AuthState, navigator: DestinationsNavigator) {
                when (state) {
                    is Ready -> navigator.navigate(HomeDestination)
                    is FatalError -> Text("Oops")
                }
            }
        """)
        self.assertEqual(["orphaned-error-state"], [item.rule for item in findings])
        self.assertIn("FatalError", findings[0].message)

    def test_qualified_types_do_not_collide_across_packages(self):
        # Two distinct sealed `State` types in different packages must never be pooled together.
        findings = self.analyze_files({
            "Checkout.kt": """
                package checkout
                sealed interface State
                data object NetworkTimeout : State
            """,
            "Profile.kt": """
                package profile
                sealed interface State
                data object Ready : State
            """,
            "Flow.kt": """
                @Destination @Composable fun CheckoutScreen(state: checkout.State) { Text(state.toString()) }
            """,
        })
        self.assertEqual(["orphaned-error-state"], [item.rule for item in findings])
        self.assertIn("checkout.State.NetworkTimeout", findings[0].message)
        self.assertNotIn("Ready", findings[0].message)

    def test_enum_constructor_argument_is_not_treated_as_a_value(self):
        findings = self.analyze("""
            enum class Choice(val tint: Color) {
                YES(Color.RED),
                NO(Color.BLUE);
            }
            @Destination @Composable fun Picker(choice: Choice, navigator: DestinationsNavigator) {
                when (choice) {
                    Choice.YES -> navigator.navigate(YesDestination)
                    Choice.NO -> navigator.navigate(NoDestination)
                }
            }
        """)
        self.assertEqual([], findings)

    def test_comma_grouped_when_branch_is_recognized_as_handled(self):
        findings = self.analyze("""
            enum class Choice { YES, NO, MAYBE }
            @Destination @Composable fun Picker(choice: Choice, navigator: DestinationsNavigator) {
                when (choice) {
                    Choice.YES, Choice.NO -> navigator.navigate(ProceedDestination)
                    Choice.MAYBE -> navigator.navigate(AskDestination)
                }
            }
        """)
        self.assertEqual([], findings)

    def test_external_target_not_in_source_set_prevents_cycle_finding(self):
        # ExternalHelpDestination isn't declared anywhere in the analyzed sources (unlike
        # test_external_exit_prevents_cycle_finding's in-file "Support"), so this exercises an
        # edge that is genuinely outside the analyzed corpus.
        findings = self.analyze("""
            @Destination @Composable fun Details(navigator: DestinationsNavigator) {
                navigator.navigate(ValidateDestination); navigator.navigate(ExternalHelpDestination)
            }
            @Destination @Composable fun Validate(navigator: DestinationsNavigator) {
                navigator.navigate(DetailsDestination)
            }
        """)
        self.assertEqual([], findings)

    def test_warning_threshold_is_configurable(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "Flow.kt"
            path.write_text("""
                @Destination(start = true) @Composable fun Home() { Text("Home") }
                @Destination @Composable fun Forgotten() { Text("Lost") }
            """, encoding="utf-8")
            with contextlib.redirect_stdout(io.StringIO()):
                default_status = ux.main([str(path), "--root", str(root)])
                strict_status = ux.main([str(path), "--root", str(root), "--fail-on", "warning"])
                report_only_status = ux.main([str(path), "--root", str(root), "--fail-on", "none"])
        self.assertEqual(0, default_status)
        self.assertEqual(1, strict_status)
        self.assertEqual(0, report_only_status)


if __name__ == "__main__":
    unittest.main()
