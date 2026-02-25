import react from 'eslint-plugin-react'
import typescriptEslint from 'typescript-eslint'
import jsxA11Y from 'eslint-plugin-jsx-a11y'
import globals from 'globals'
import tsParser from '@typescript-eslint/parser'
// import js from '@eslint/js'

export default [
  //js.configs.recommended, // TODO: Her må vi gjøre endringer før vi kommenterer denne inn igjen.
  ...typescriptEslint.configs.recommended,
  {
    files: ['src/**/*.ts', 'src/**/*.tsx'],
    ignores: ['node_modules/*', 'nais/*', 'build/*'],
    plugins: {
      react: react,
      '@typescript-eslint': typescriptEslint.plugin,
      'jsx-a11y': jsxA11Y,
    },
    languageOptions: {
      globals: {
        ...globals.browser,
        process: true,
        require: true,
        module: true,
        __dirname: true,
      },

      parser: tsParser,
      ecmaVersion: 15,
      sourceType: 'module',

      parserOptions: {
        ecmaFeatures: {
          jsx: true,
        },
      },
    },
    settings: {
      react: {
        version: '18.3.1',
      },
    },

    rules: {
      'react/jsx-curly-brace-presence': 'error',
      'react/react-in-jsx-scope': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      'react/prop-types': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/no-var-requires': 'off',
      '@typescript-eslint/no-empty-function': 'off',
      'react/display-name': 'off',
      '@typescript-eslint/no-empty-interface': 'off',
      '@typescript-eslint/no-extra-non-null-assertion': 'off',
      '@typescript-eslint/ban-types': 'off',
      '@typescript-eslint/no-non-null-assertion': 'off',
      'react-hooks/exhaustive-deps': 'off',
      '@typescript-eslint/no-extra-semi': 'off',
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': 'error',
    },
  },
]
