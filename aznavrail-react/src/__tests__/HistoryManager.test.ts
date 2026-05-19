import { historyManager } from '../util/HistoryManager';

describe('HistoryManager', () => {
  beforeEach(() => {
    // Reset internal state before each test to ensure isolation.
    (historyManager as any).history = {};
    // Reset limit to default value, as it can be modified by tests.
    historyManager.setLimit(5);
  });

  describe('getSuggestions', () => {
    it('returns empty array if the query is empty (empty query never matches)', () => {
      // Failure: getSuggestions must short-circuit on a falsy query; the !query guard
      // in HistoryManager.getSuggestions is missing or has wrong polarity.
      const context = 'empty-query-ctx';
      historyManager.addEntry(context, 'Apple');
      expect(historyManager.getSuggestions(context, '')).toEqual([]);
    });

    it('returns empty array if there is no history for the context (unknown context returns [])', () => {
      // Failure: getSuggestions reads from this.history[context] || []; if it returns
      // undefined-mapped values the `|| []` fallback is missing.
      const context = 'no-history-ctx';
      expect(historyManager.getSuggestions(context, 'A')).toEqual([]);
    });

    it('returns matching suggestions case-insensitively (toLowerCase on both sides)', () => {
      // Failure: If "APP" does not match "Apple", the filter's toLowerCase() call is
      // missing on one of the operands.
      const context = 'case-insensitive-ctx';
      historyManager.addEntry(context, 'Apple');
      historyManager.addEntry(context, 'Banana');
      historyManager.addEntry(context, 'Avocado');

      let suggestions = historyManager.getSuggestions(context, 'a');
      expect(suggestions).toEqual(['Avocado', 'Banana', 'Apple']); // most-recent first

      suggestions = historyManager.getSuggestions(context, 'APP');
      expect(suggestions).toEqual(['Apple']);
    });

    it('returns empty array if no matches are found (filter returns [] when nothing matches)', () => {
      // Failure: If this returns non-empty, the filter predicate is inverted.
      const context = 'no-matches-ctx';
      historyManager.addEntry(context, 'Apple');
      expect(historyManager.getSuggestions(context, 'Zebra')).toEqual([]);
    });

    it('returns matches only from the specified context (history is namespaced by context key)', () => {
      // Failure: If "Avocado" leaks into context1, the per-context dictionary is being
      // searched globally; check `this.history[context]` lookup, not Object.values().
      const context1 = 'context-1';
      const context2 = 'context-2';

      historyManager.addEntry(context1, 'Apple');
      historyManager.addEntry(context2, 'Avocado');

      const suggestions = historyManager.getSuggestions(context1, 'a');
      expect(suggestions).toEqual(['Apple']);
    });

    it('uses "global" context by default if no context is provided (undefined → "global")', () => {
      // Failure: The default parameter value `context: string = 'global'` is wrong or
      // missing on addEntry/getSuggestions.
      historyManager.addEntry(undefined, 'GlobalApple');
      const suggestions = historyManager.getSuggestions(undefined, 'GlobalA');
      expect(suggestions).toEqual(['GlobalApple']);
    });
  });

  describe('ranking', () => {
    it('orders suggestions by most-recent-first (newest addEntry is at index 0)', () => {
      // Failure: If "Apple" appears at index 0, addEntry is push()ing instead of unshift()ing.
      const ctx = 'rank-recent';
      historyManager.addEntry(ctx, 'Apple');
      historyManager.addEntry(ctx, 'Apricot');
      historyManager.addEntry(ctx, 'Avocado');
      const suggestions = historyManager.getSuggestions(ctx, 'a');
      expect(suggestions).toEqual(['Avocado', 'Apricot', 'Apple']);
    });

    it('re-adding an existing entry moves it to the front (de-dup by splice + unshift)', () => {
      // Failure: If duplicates exist or order is unchanged, the dedupe branch
      // (indexOf > -1 → splice) is missing in addEntry.
      const ctx = 'rank-promote';
      historyManager.addEntry(ctx, 'Apple');
      historyManager.addEntry(ctx, 'Apricot');
      historyManager.addEntry(ctx, 'Avocado');
      // Promote "Apple" back to the front
      historyManager.addEntry(ctx, 'Apple');
      const suggestions = historyManager.getSuggestions(ctx, 'a');
      // De-dup means Apple appears once, at the front.
      expect(suggestions).toEqual(['Apple', 'Avocado', 'Apricot']);
      expect(suggestions.filter((s) => s === 'Apple').length).toBe(1);
    });

    it('ignores entries that contain only whitespace (trim()-empty strings are dropped)', () => {
      // Failure: If whitespace entries are stored, the `if (!entry.trim()) return;` guard
      // in addEntry is missing.
      const ctx = 'rank-whitespace';
      historyManager.addEntry(ctx, '   ');
      historyManager.addEntry(ctx, '\t\n');
      historyManager.addEntry(ctx, 'Apple');
      expect(historyManager.getSuggestions(ctx, 'a')).toEqual(['Apple']);
    });
  });

  describe('namespacing by context', () => {
    it('different contexts have independent histories (no cross-contamination)', () => {
      // Failure: If suggestions from one context appear in another, the lookup is using
      // a shared array rather than per-context dictionaries.
      historyManager.addEntry('ctxA', 'Alpha');
      historyManager.addEntry('ctxB', 'Beta');
      historyManager.addEntry('ctxA', 'Apple');
      expect(historyManager.getSuggestions('ctxA', '')).toEqual([]);
      expect(historyManager.getSuggestions('ctxA', 'a')).toEqual(['Apple', 'Alpha']);
      expect(historyManager.getSuggestions('ctxB', 'b')).toEqual(['Beta']);
      // ctxA-only entries ("Apple", "Alpha") must not leak into ctxB. Use substrings unique
      // to those entries — note: query 'a' would match 'Beta' (case-insensitive substring),
      // so it isn't a useful cross-context test.
      expect(historyManager.getSuggestions('ctxB', 'pple')).toEqual([]);
      expect(historyManager.getSuggestions('ctxB', 'lph')).toEqual([]);
    });

    it('contexts are case-sensitive ("MyCtx" and "myctx" are different keys)', () => {
      // Failure: If both contexts return the same data, the context key is being
      // lowercased before lookup; per spec, only the query is normalised, not the key.
      historyManager.addEntry('MyCtx', 'Apple');
      expect(historyManager.getSuggestions('myctx', 'a')).toEqual([]);
      expect(historyManager.getSuggestions('MyCtx', 'a')).toEqual(['Apple']);
    });
  });

  describe('eviction at limit', () => {
    it('evicts the oldest entry once the per-context limit is exceeded (LRU pop from the tail)', () => {
      // Failure: If "A" remains, the `if (list.length > this.limit) list.pop()` line is
      // missing; if "E" is evicted, pop is shift (wrong end).
      historyManager.setLimit(3);
      const ctx = 'evict-1';
      historyManager.addEntry(ctx, 'A');
      historyManager.addEntry(ctx, 'B');
      historyManager.addEntry(ctx, 'C');
      historyManager.addEntry(ctx, 'D');
      // Expect [D, C, B], with A evicted.
      expect(historyManager.getSuggestions(ctx, '')).toEqual([]);
      expect(historyManager.getSuggestions(ctx, 'A')).toEqual([]);
      expect(historyManager.getSuggestions(ctx, 'B')).toEqual(['B']);
      expect(historyManager.getSuggestions(ctx, 'D')).toEqual(['D']);
    });

    it('limit is configurable via setLimit (limit=1 keeps only the latest entry)', () => {
      // Failure: If multiple entries survive, setLimit is not updating the field used
      // by the addEntry eviction check.
      historyManager.setLimit(1);
      const ctx = 'evict-limit-1';
      historyManager.addEntry(ctx, 'first');
      historyManager.addEntry(ctx, 'second');
      historyManager.addEntry(ctx, 'third');
      expect(historyManager.getSuggestions(ctx, 'first')).toEqual([]);
      expect(historyManager.getSuggestions(ctx, 'second')).toEqual([]);
      expect(historyManager.getSuggestions(ctx, 'third')).toEqual(['third']);
    });

    it('changing the limit does not retroactively evict entries already over the new limit', () => {
      // Failure: setLimit is documented as affecting future addEntry calls only; if it
      // immediately truncates the list, the implementation has added retroactive trimming.
      const ctx = 'evict-retro';
      historyManager.setLimit(5);
      historyManager.addEntry(ctx, 'A');
      historyManager.addEntry(ctx, 'B');
      historyManager.addEntry(ctx, 'C');
      historyManager.setLimit(1);
      // 3 entries existed before the new limit; they should still be queryable.
      const sugg = historyManager.getSuggestions(ctx, '');
      expect(sugg).toEqual([]); // empty query → []
      expect(historyManager.getSuggestions(ctx, 'a')).toEqual(['A']);
      expect(historyManager.getSuggestions(ctx, 'c')).toEqual(['C']);
    });

    it('eviction is per-context (filling one context does not evict from another)', () => {
      // Failure: If ctxB loses entries when ctxA fills up, the limit is being applied
      // across the union of all contexts.
      historyManager.setLimit(2);
      historyManager.addEntry('ctxA', 'A1');
      historyManager.addEntry('ctxA', 'A2');
      historyManager.addEntry('ctxA', 'A3'); // evicts A1
      historyManager.addEntry('ctxB', 'B1');
      historyManager.addEntry('ctxB', 'B2');
      expect(historyManager.getSuggestions('ctxA', 'a')).toEqual(['A3', 'A2']);
      expect(historyManager.getSuggestions('ctxB', 'b')).toEqual(['B2', 'B1']);
    });
  });
});
