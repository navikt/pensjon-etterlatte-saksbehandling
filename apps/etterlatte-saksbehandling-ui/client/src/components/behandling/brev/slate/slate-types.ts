import { HistoryEditor } from 'slate-history'
import { BaseEditor } from 'slate'
import { ReactEditor } from 'slate-react'

declare module 'slate' {
  interface CustomTypes {
    Editor: BaseEditor & ReactEditor & HistoryEditor
    Element: CustomElement | CustomHeading | CustomQuote | CustomList
    Text: CustomText
  }
}

export const SLATE_HOTKEYS = {
  'mod+b': 'bold',
  'mod+i': 'italic',
  'mod+u': 'underline',
  'mod+`': 'code',
}

export type CustomElement = { [key: string]: any; type: 'paragraph'; align?: string; children: CustomText[] }
export type CustomHeading = { [key: string]: any; type: 'heading'; align?: string; children: CustomText[] }
export type CustomQuote = { [key: string]: any; type: 'block-quote'; align?: string; children: CustomText[] }
export type CustomList = { [key: string]: any; type: 'list-item'; align?: string; children: CustomText[] }
export type CustomText = {
  key: string
  text: string
  bold?: boolean
  italic?: boolean
  underline?: boolean
  light?: boolean
  change?: boolean
  clear?: boolean
}

export type ElementTypes = 'paragraph' | 'heading' | 'block-quote' | 'list-item' | undefined

export const LIST_TYPES = ['numbered-list', 'bulleted-list']
