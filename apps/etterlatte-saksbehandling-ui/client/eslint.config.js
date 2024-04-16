import react from 'eslint-plugin-react'
import typescript from '@typescript-eslint/eslint-plugin'
import jsxA11y from 'eslint-plugin-jsx-a11y'
import typescriptParser from '@typescript-eslint/parser'

const config = [
  {
    ...react.configs.recommended,
    ...typescript.configs.recommended,
    settings: {
      react: {
        version: '18.2.0',
      },
    },
    ignores: ['node_modules/*', 'nais/*', 'build/*'],
    languageOptions: {
      parser: typescriptParser,
      parserOptions: {
        sourceType: 'module',
      },
      ecmaFeatures: {
        jsx: true,
      },
      ecmaVersion: 12,
      globals: {
        browser: true,
        es2021: true,
        process: true,
        require: true,
        module: true,
        __dirname: true,
      },
    },
    plugins: {
      ...react,
      ...typescript,
      ...jsxA11y,
    },
    files: ['**/*.ts', '**/*.tsx'],
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

export default config
