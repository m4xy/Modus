import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import prettier from 'eslint-config-prettier';

/**
 * Rule 5 of the brief: nothing outside `src/api` may reach for the network.
 *
 * `no-restricted-globals` only matches unqualified identifiers, so it is half a
 * mechanism on its own — `window.fetch(...)` walks straight past it. The two
 * lists below are paired deliberately: the globals list catches the bare
 * identifier and the constructors (`new EventSource`, `new WebSocket`), and the
 * properties list catches every qualified route to the same APIs plus
 * `navigator.sendBeacon`, which is a network call that does not look like one.
 */
const NETWORK_MESSAGE = 'Use the typed client in src/api instead of reaching for the network here.';

const NETWORK_GLOBALS = ['fetch', 'EventSource', 'WebSocket', 'XMLHttpRequest'];

const GLOBAL_OBJECTS = ['window', 'globalThis', 'self'];

const restrictedProperties = [
  ...GLOBAL_OBJECTS.flatMap((object) =>
    NETWORK_GLOBALS.map((property) => ({ object, property, message: NETWORK_MESSAGE })),
  ),
  { object: 'navigator', property: 'sendBeacon', message: NETWORK_MESSAGE },
];

export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'public/mockServiceWorker.js', 'coverage'] },
  js.configs.recommended,
  ...tseslint.configs.recommendedTypeChecked,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        {
          allowConstantExport: true,
          // Context hooks and the icon map live beside their provider on purpose.
          allowExportNames: ['useTheme', 'useDomain', 'useToast', 'icons'],
        },
      ],
      '@typescript-eslint/consistent-type-imports': ['error', { fixStyle: 'inline-type-imports' }],
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'no-restricted-globals': [
        'error',
        ...NETWORK_GLOBALS.map((name) => ({ name, message: NETWORK_MESSAGE })),
      ],
      'no-restricted-properties': ['error', ...restrictedProperties],
    },
  },
  {
    // The API client is the one place allowed to talk to the network. The SSE
    // client (0003) is deliberately absent: it has to add itself here on
    // purpose rather than inherit an exemption written before it existed.
    files: ['src/api/http.ts', 'src/mocks/**/*.ts'],
    rules: { 'no-restricted-globals': 'off', 'no-restricted-properties': 'off' },
  },
  {
    files: ['**/*.js'],
    ...tseslint.configs.disableTypeChecked,
    languageOptions: { globals: globals.node },
  },
  prettier,
);
