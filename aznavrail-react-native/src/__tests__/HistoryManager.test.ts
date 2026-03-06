import { historyManager } from '../util/HistoryManager';

describe('HistoryManager', () => {
  beforeEach(() => {
    // Reset internal state by clearing the history record.
    // We can do this by using a workaround or just adding specific test entries.
    // Since we can't easily clear history via public API, we'll use unique contexts for tests.
  });

  describe('getSuggestions', () => {
    it('returns empty array if the query is empty', () => {
      const context = 'empty-query-ctx';
      historyManager.addEntry(context, 'Apple');
      expect(historyManager.getSuggestions(context, '')).toEqual([]);
    });

    it('returns empty array if there is no history for the context', () => {
      const context = 'no-history-ctx';
      expect(historyManager.getSuggestions(context, 'A')).toEqual([]);
    });

    it('returns matching suggestions case-insensitively', () => {
      const context = 'case-insensitive-ctx';
      historyManager.addEntry(context, 'Apple');
      historyManager.addEntry(context, 'Banana');
      historyManager.addEntry(context, 'Avocado');

      // Query "a"
      let suggestions = historyManager.getSuggestions(context, 'a');
      expect(suggestions).toEqual(['Avocado', 'Banana', 'Apple']); // Ordered by most recent

      // Query "APP"
      suggestions = historyManager.getSuggestions(context, 'APP');
      expect(suggestions).toEqual(['Apple']);
    });

    it('returns empty array if no matches are found', () => {
      const context = 'no-matches-ctx';
      historyManager.addEntry(context, 'Apple');
      expect(historyManager.getSuggestions(context, 'Zebra')).toEqual([]);
    });

    it('returns matches only from the specified context', () => {
      const context1 = 'context-1';
      const context2 = 'context-2';

      historyManager.addEntry(context1, 'Apple');
      historyManager.addEntry(context2, 'Avocado');

      // Query "A" on context1
      const suggestions = historyManager.getSuggestions(context1, 'a');
      expect(suggestions).toEqual(['Apple']); // Avocado shouldn't be here
    });

    it('uses "global" context by default if no context is provided', () => {
      historyManager.addEntry(undefined, 'GlobalApple');
      const suggestions = historyManager.getSuggestions(undefined, 'GlobalA');
      expect(suggestions).toEqual(['GlobalApple']);
    });
  });
});
