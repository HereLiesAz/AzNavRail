# Security Policy

## Supported Versions

AzNavRail is always about the **latest** release. Only the most recent published version receives
security updates — older versions are not supported. Pull the newest tag from JitPack (Android) or
npm (`@HereLiesAz/aznavrail-react`) before reporting.

## Reporting a Vulnerability

Open a security advisory or issue on the [GitHub repository](https://github.com/HereLiesAz/AzNavRail)
describing the vulnerability and the steps to reproduce it. You can expect an initial response within
a few days; accepted issues are fixed in the next release and credited in the changelog.

## Known accepted advisory: `brace-expansion` (dev-scope)

`npm audit` in `aznavrail-react` reports GHSA `brace-expansion` DoS (unbounded expansion length) on
copies nested under ESLint's config machinery, `eslint-plugin-react`, `test-exclude`, and `glob`.
There is no backported fix: the advisory covers `<= 5.0.7`, so only the 5.0.8 major is clean, and
`brace-expansion` 5 replaced the default-export function with a named export.

A blanket `overrides` pin to `^5.0.8` therefore **breaks the build**: `minimatch` 3.x — bundled by
both `react-native-builder-bob`'s `glob` and ESLint — calls it as a function, so `npm run build` and
`npm run lint` both die with `TypeError: expand is not a function`. That pin was in place once, and
because the Pages workflow ran `bob build || true` it failed silently and shipped a stale `lib/`.

Every remaining vulnerable copy is a build-time glob matcher fed our own patterns, never untrusted
input, and none of them reach a published artifact. The advisory is accepted rather than pinned. It
resolves on its own when ESLint and `glob` move to `brace-expansion` 5.

Everything else Dependabot flagged on this package was resolved by the React Native 0.86 / React 19
upgrade rather than by pinning: `shell-quote`, `ws`, `js-yaml`, `flatted`, `yaml`, `joi`,
`fast-xml-parser`, and the `@babel/*` advisories are all gone from the tree.
