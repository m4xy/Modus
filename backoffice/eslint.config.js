import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import prettier from 'eslint-config-prettier';

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
      // Rule 5 of the brief: nothing outside src/api may reach for the network.
      'no-restricted-globals': [
        'error',
        {
          name: 'fetch',
          message: 'Use the typed client in src/api instead of calling fetch directly.',
        },
      ],
    },
  },
  {
    // The API client is the one place allowed to talk to the network.
    files: ['src/api/http.ts', 'src/mocks/**/*.ts'],
    rules: { 'no-restricted-globals': 'off' },
  },
  {
    files: ['**/*.js'],
    ...tseslint.configs.disableTypeChecked,
    languageOptions: { globals: globals.node },
  },
  prettier,
);
