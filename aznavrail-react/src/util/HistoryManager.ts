/** In-memory store for per-context typed-history entries used by `AzTextBox` autocomplete suggestions. */
class HistoryManager {
  private history: Record<string, string[]> = {};
  private limit: number = 5;

  /** Sets the maximum number of entries retained per context key. */
  setLimit(limit: number) {
    this.limit = limit;
  }

  /** Adds `entry` to the history for `context`, moving it to the front and enforcing the limit. */
  addEntry(context: string = 'global', entry: string) {
    if (!entry.trim()) return;
    if (!this.history[context]) {
      this.history[context] = [];
    }
    const list = this.history[context];
    const index = list.indexOf(entry);
    if (index > -1) {
      list.splice(index, 1);
    }
    list.unshift(entry);
    if (list.length > this.limit) {
      list.pop();
    }
  }

  /** Returns all history entries for `context` that contain `query` (case-insensitive). */
  getSuggestions(context: string = 'global', query: string): string[] {
    const list = this.history[context] || [];
    if (!query) return [];
    return list.filter(item => item.toLowerCase().includes(query.toLowerCase()));
  }
}

/** Singleton `HistoryManager` instance shared across all `AzTextBox` components. */
export const historyManager = new HistoryManager();
