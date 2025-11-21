class HistoryManager {
  history = {};
  limit = 5;
  setLimit(limit) {
    this.limit = limit;
  }
  addEntry(context = 'global', entry) {
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
  getSuggestions(context = 'global', query) {
    const list = this.history[context] || [];
    if (!query) return [];
    return list.filter(item => item.toLowerCase().includes(query.toLowerCase()));
  }
}
export const historyManager = new HistoryManager();
//# sourceMappingURL=HistoryManager.js.map