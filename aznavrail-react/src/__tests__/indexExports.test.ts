import { AzUnattachedHostItem, AzUnattachedAnchor } from '../index';

/** Confirms the new public symbols reach consumers through the package's single entry point. */
describe('index exports', () => {
  it('exposes AzUnattachedHostItem and AzUnattachedAnchor', () => {
    expect(typeof AzUnattachedHostItem).toBe('function');
    expect(AzUnattachedAnchor.FLOATING).toBe('FLOATING');
    expect(AzUnattachedAnchor.OPPOSITE).toBe('OPPOSITE');
    expect(AzUnattachedAnchor.BOTTOM).toBe('BOTTOM');
  });
});
