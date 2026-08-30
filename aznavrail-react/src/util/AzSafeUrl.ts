/**
 * Whether `url` is safe to hand to React Native's `Linking.openURL`.
 *
 * On web (react-native-web maps `Linking.openURL` onto browser navigation) a `javascript:` URL
 * executes immediately, and native platforms have their own scheme-abuse risks (e.g. `intent:`).
 * Every URL this library opens that did not come from a compile-time string literal — a markdown
 * link, a fetched "more from Az" catalog entry, a dropdown item's declared route, a rail item's
 * `route` — has to pass this check first; it must never be assumed safe just because it looks like
 * a link.
 */
export function isSafeExternalUrl(url: string): boolean {
  return (
    url.startsWith('http://') ||
    url.startsWith('https://') ||
    url.startsWith('mailto:')
  );
}
