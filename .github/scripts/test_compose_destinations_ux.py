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
