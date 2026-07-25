// ESLint 9 flat config. The package used to configure ESLint through the `eslintConfig` /
// `eslintIgnore` keys in package.json — the eslintrc format, which ESLint 9 no longer reads.
const reactNative = require('@react-native/eslint-config/flat');
const prettierConfig = require('eslint-config-prettier');
const prettierPlugin = require('eslint-plugin-prettier');
const tsPlugin = require('@typescript-eslint/eslint-plugin');

const prettierOptions = {
  quoteProps: 'consistent',
  singleQuote: true,
  tabWidth: 2,
  trailingComma: 'es5',
  useTabs: false,
};

module.exports = [
  {
    ignores: ['node_modules/**', 'lib/**', 'showcase/**'],
  },
  ...reactNative,
  prettierConfig,
  {
    plugins: { prettier: prettierPlugin },
    rules: {
      'prettier/prettier': ['error', prettierOptions],
    },
  },
  {
    // A leading underscore is this codebase's way of saying "destructured to discard".
    files: ['**/*.{ts,tsx}'],
    plugins: { '@typescript-eslint': tsPlugin },
    rules: {
      '@typescript-eslint/no-unused-vars': [
        'warn',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
    },
  },
  {
    // Jest globals. The RN config only claims the conventional `__tests__` layout, and this
    // package also keeps `*.test.jsx` beside the web components they cover.
    files: [
      '**/__tests__/**',
      '**/*.test.{js,jsx,ts,tsx}',
      'jest.config.js',
      'jest.style-mock.js',
    ],
    languageOptions: {
      globals: {
        afterAll: 'readonly',
        afterEach: 'readonly',
        beforeAll: 'readonly',
        beforeEach: 'readonly',
        describe: 'readonly',
        expect: 'readonly',
        it: 'readonly',
        jest: 'readonly',
        test: 'readonly',
      },
    },
  },
];
