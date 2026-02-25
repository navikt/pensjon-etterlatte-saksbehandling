import typescriptEslint from 'typescript-eslint'
import globals from 'globals'
import tsParser from '@typescript-eslint/parser'
import js from '@eslint/js'
import prettier from 'eslint-plugin-prettier'
import jest from 'eslint-plugin-jest'

export default [
  js.configs.recommended,
  ...typescriptEslint.configs.recommended,

  {
    files: ['**/*.ts', '**/*.tsx'],
    plugins: {
      '@typescript-eslint': typescriptEslint.plugin,
      prettier: prettier,
      jest: jest,
    },
    languageOptions: {
      globals: {
        ...globals.node,
        ...jest.environments.globals.globals,
      },
      parser: tsParser,
      ecmaVersion: 15,
      sourceType: 'module',
    },
    rules: {
      'no-console': 'error',
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': 'error',
      '@typescript-eslint/no-explicit-any': 'error',
    },
  },
]
