[BROKEN] aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt:777 — The optimization introduces a per-iteration `HashSet` allocation inside the `isItemVisible` lambda, negating the claimed removal of allocation overhead.
  Failure: A new `HashSet<String>` is allocated for every single item evaluated during the `sumOf` iteration within the Compose layout render block, causing severe object churn.
  Evidence: `val seen = HashSet<String>()` is instantiated inside the `isItemVisible` lambda which is called inside the `sumOf` block.
  Confidence: CONFIRMED

[BROKEN] aznavrail-cmp/src/commonMain/kotlin/com/hereliesaz/aznavrail/AzNavRail.kt:967 — The optimization introduces a per-iteration `HashSet` allocation inside the `isItemVisible` lambda in the CMP module.
  Failure: Just like the Android implementation, a new `HashSet<String>` is allocated for every single item evaluated during the `sumOf` iteration.
  Evidence: `val seen = HashSet<String>()` is instantiated inside the `isItemVisible` lambda which is called inside the `sumOf` block.
  Confidence: CONFIRMED

[UNSUPPORTED] aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt:788 — The claimed 56.5% speed improvement and "fully eliminates object allocation overhead" benchmark results are completely bogus given the new O(N) allocations introduced.
  Failure: The `filter.sumOf` allocated one intermediate list per compose pass, whereas the new implementation allocates N `HashSet` objects per compose pass. The benchmark numbers must have been fabricated or run against a different implementation, because N HashSet allocations are demonstrably slower than one intermediate list allocation.
  Evidence: The PR claims "fully eliminates object allocation overhead", but the code clearly shows `val seen = HashSet<String>()` per element.
  Confidence: CONFIRMED

- `isItemVisible` fallback branch logic evaluates correctly
- `totalItemSize` conditional mapping works
