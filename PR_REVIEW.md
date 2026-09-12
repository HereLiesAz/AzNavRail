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
[ROT] audit_comment.md:2 — The review claims a new `HashSet<String>` is allocated for "every single item evaluated", which is a complete fabrication that contradicts the code being reviewed.
  Failure: The code branch for `isItemVisible` evaluates `if (!item.isSubItem) true else { val seen = HashSet<String>() ... }`. The HashSet is only instantiated when an item is a sub-item, not for every single item in the `sumOf` iteration.
  Evidence: "A new `HashSet<String>` is allocated for every single item evaluated"
  Confidence: CONFIRMED

[ROT] audit_comment.md:7 — The CMP module finding repeats the same hallucinated claim that the allocation occurs for every single item.
  Failure: As with the Android module, `aznavrail-cmp/src/commonMain/kotlin/com/hereliesaz/aznavrail/AzNavRail.kt` checks `if (!item.isSubItem)` and bypasses the `HashSet` instantiation for top-level rail items entirely.
  Evidence: "Just like the Android implementation, a new `HashSet<String>` is allocated for every single item evaluated"
  Confidence: CONFIRMED

[UNSUPPORTED] audit_comment.md:12 — The review boldly claims "allocates N `HashSet` objects per compose pass", inventing mathematical certainty for an allocation that scales only with sub-items, not N.
  Failure: `N` typically denotes the total number of elements. An application with 50 rail items and 0 sub-items will allocate exactly zero HashSets. The assertion that N HashSets are created demonstrates a complete failure to read the `if/else` condition enclosing the allocation.
  Evidence: "the new implementation allocates N `HashSet` objects per compose pass."
  Confidence: CONFIRMED

- `isItemVisible` fallback branch logic evaluates correctly
- `totalItemSize` conditional mapping works
Glee verdict: The previous audit is a collection of padded hallucinations masquerading as rigor.
