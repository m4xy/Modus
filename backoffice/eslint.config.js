import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import importPlugin from 'eslint-plugin-import';
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
            import: importPlugin,
        },
        settings: {
            // `import/parsers` is what makes `no-cycle` real: without it the plugin
            // cannot parse a TypeScript dependency, so it follows nothing and reports
            // nothing — green on a planted cx.ts <-> Tabs.tsx cycle (bean:0046).
            ...importPlugin.flatConfigs.typescript.settings,
            // The TypeScript resolver, so `./App` resolves to `App.tsx` and a cycle
            // through it is seen rather than skipped as unresolvable.
            'import/resolver': { typescript: { project: './tsconfig.json' } },
        },
        rules: {
            ...reactHooks.configs.recommended.rules,
            'import/no-cycle': 'error',
            // `!` is the same unproven assumption Detekt bans as `!!` in Kotlin
            // (doc:20-ddd-practices#domain-prohibitions). Set as its own rule rather
            // than by swapping recommendedTypeChecked for strictTypeChecked, which
            // would turn on twenty unrelated rules this work item never assessed.
            '@typescript-eslint/no-non-null-assertion': 'error',
            'react-refresh/only-export-components': [
                'warn',
                {
                    allowConstantExport: true,
                    // Context hooks and the icon map live beside their provider on purpose.
                    allowExportNames: ['useTheme', 'useDomain', 'useToast', 'icons'],
                },
            ],
            '@typescript-eslint/consistent-type-imports': [
                'error',
                { fixStyle: 'inline-type-imports' },
            ],
            '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
            'no-restricted-globals': [
                'error',
                ...NETWORK_GLOBALS.map((name) => ({ name, message: NETWORK_MESSAGE })),
            ],
            'no-restricted-properties': ['error', ...restrictedProperties],
        },
    },
    {
        // Accessibility is a deliverable (doc:00-constitution §10), and the axe
        // assertions in e2e/tests/accessibility.spec.ts only run against a built and
        // running system. This is the half that runs in the fast gate. Every rule in
        // `recommended` is already `error`; nothing here relaxes one.
        files: ['**/*.tsx'],
        ...jsxA11y.flatConfigs.recommended,
        rules: {
            ...jsxA11y.flatConfigs.recommended.rules,
            // `tabpanel` is in this rule's default allowlist because a scrollable
            // panel has to be keyboard-scrollable (WCAG 2.1.1). The agent transcript
            // is the same shape — role="log", 26rem tall, overflow-y: auto, measured
            // at scrollHeight 1119 against clientHeight 416 — so `log` is allowed for
            // the same reason and nothing else is. axe accepts the region with or
            // without the tabindex (`scrollable-region-focusable` passes either way,
            // observed in bean:0046), so deleting it to satisfy this rule would have
            // removed real keyboard access that no other gate would have missed.
            'jsx-a11y/no-noninteractive-tabindex': ['error', { roles: ['tabpanel', 'log'] }],
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
