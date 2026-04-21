declare class HistoryManager {
    private history;
    private limit;
    setLimit(limit: number): void;
    addEntry(context: string | undefined, entry: string): void;
    getSuggestions(context: string | undefined, query: string): string[];
}
export declare const historyManager: HistoryManager;
export {};
//# sourceMappingURL=HistoryManager.d.ts.map