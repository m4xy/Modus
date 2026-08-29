import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import importPlugin from 'eslint-plugin-import';
import prettier from 'eslint-config-prettier';

/**
 * `e2e/` has its own flat config rather than being reached from
 * `backoffice/eslint.config.js`: ESLint 9 resolves a flat config from the working
 * directory and ignores files outside its base path, and the two trees do not share a
 * rule set anyway — there is no React and no JSX here, and a Playwright test body runs
 * in Node while a `page.evaluate` callback runs in the browser, so both global sets are
 * in scope. The `lint` script in `backoffice/package.json` chains this one, exactly as
 * `typecheck` and `format:check` already do, so one `npm run lint` and the one
 * `backofficeLint` Gradle task both cover both trees (bean:0046).
 */
export default tseslint.config(
    { ignores: ['node_modules', 'playwright-report', 'test-results'] },
    js.configs.recommended,
    ...tseslint.configs.recommendedTypeChecked,
    {
        files: ['**/*.ts'],
        languageOptions: {
            ecmaVersion: 2023,
            // Test bodies are Node; `page.evaluate` callbacks are typed against the DOM.
            globals: { ...globals.node, ...globals.browser },
            parserOptions: {
                projectService: true,
                tsconfigRootDir: import.meta.dirname,
            },
        },
        plugins: { import: importPlugin },
        settings: {
            // `import/parsers` is what makes `no-cycle` real: without it the plugin
            // cannot parse a TypeScript dependency, so it follows nothing and reports
            // nothing — green on a planted two-file cycle (bean:0046).
            ...importPlugin.flatConfigs.typescript.settings,
            // The TypeScript resolver, so a relative import resolves to its `.ts` file
            // and a cycle through it is seen rather than skipped as unresolvable.
            'import/resolver': { typescript: { project: './tsconfig.json' } },
        },
        rules: {
            'import/no-cycle': 'error',
            '@typescript-eslint/no-non-null-assertion': 'error',
            '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
        },
    },
    {
        files: ['**/*.js'],
        ...tseslint.configs.disableTypeChecked,
        languageOptions: { globals: globals.node },
    },
    prettier,
);
