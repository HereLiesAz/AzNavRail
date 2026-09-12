[UNSOUND] aznavrail/src/test/java/com/hereliesaz/aznavrail/util/HistoryManagerUtf8Test.kt:8 — A second copy of a single source of truth restates the implementation instead of testing the code.
  Failure: The test suite defines its own local copy of `utf8ByteLength` and asserts against it, completely ignoring the production code in `HistoryManager.kt`. Reintroducing the bug in production will leave the tests perfectly green.
  Evidence: `private fun String.utf8ByteLength(): Int` is explicitly defined in the test file on line 8, and the assertions test this mock rather than the manager.
  Confidence: CONFIRMED

[UNSOUND] aznavrail/src/test/java/com/hereliesaz/aznavrail/util/HistoryManagerSaveBenchmark.kt:64 — A second copy of a single source of truth was left behind to test discarded code.
  Failure: The benchmark tests a stale, isolated copy of `utf8ByteLength` that lacks the bug fix introduced in this PR, rendering its performance claims meaningless for the current implementation.
  Evidence: The benchmark file contains its own `private fun utf8ByteLength(s: String): Int` at line 64 which uses the old, bugged surrogate logic.
  Confidence: CONFIRMED

Checked and found sound:
- Standard UTF-8 replacement logic using 1 byte for unpaired surrogates correctly matches `.toByteArray(Charsets.UTF_8)` behavior.
- Out of bounds crash correctly prevented by robust `i + 1 < length` checks.

Verdict: Incomplete and unsound. Tests pass only because they test a copy of the code, not the code itself.
