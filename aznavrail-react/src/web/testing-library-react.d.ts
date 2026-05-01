// Minimal type declarations for `@testing-library/react` so this package's
// `web/*.test.tsx` files typecheck without requiring the dep to be installed
// in environments where it can't be (e.g. FAT32 dev volumes that block npm
// symlinks). The real package's types take precedence when installed.
declare module '@testing-library/react' {
  import { ReactElement } from 'react';
  export function render(ui: ReactElement): {
    container: HTMLElement;
    unmount: () => void;
    rerender: (ui: ReactElement) => void;
    asFragment: () => DocumentFragment;
    getByText: (text: string) => HTMLElement;
    queryByText: (text: string) => HTMLElement | null;
    findByText: (text: string) => Promise<HTMLElement>;
    [key: string]: any;
  };
  export function act<T>(callback: () => T | Promise<T>): Promise<T>;
  export function fireEvent(element: HTMLElement, event: Event): void;
  export const screen: any;
}
