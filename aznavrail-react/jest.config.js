// Two suites live in this package and they need different worlds, so they are declared as two
// projects rather than fought over in one config.
//
//   native — the React Native components, run under the RN preset in a node environment.
//   web    — the plain-DOM components under `src/web`, run under jsdom with react-dom.
//
// Running them as one project fails outright: the RN preset's setup defines `window` on the
// global, which jsdom has already defined and locked.
const styleMock = '<rootDir>/jest.style-mock.js';
const styleMatcher = '\\.(css|less|sass|scss)$';

module.exports = {
  projects: [
    {
      displayName: 'native',
      preset: 'react-native',
      testMatch: [
        '<rootDir>/__tests__/**/*.test.{ts,tsx}',
        '<rootDir>/src/**/__tests__/**/*.test.{ts,tsx}',
      ],
      moduleNameMapper: { [styleMatcher]: styleMock },
      transformIgnorePatterns: [
        'node_modules/(?!((jest-)?react-native|@react-native(-community)?|test-renderer)/)',
      ],
    },
    {
      displayName: 'web',
      testEnvironment: 'jsdom',
      testMatch: ['<rootDir>/src/web/**/*.test.{js,jsx,ts,tsx}'],
      moduleNameMapper: { [styleMatcher]: styleMock },
      transform: { '\\.[jt]sx?$': 'babel-jest' },
    },
  ],
  modulePathIgnorePatterns: ['<rootDir>/lib/'],
};
