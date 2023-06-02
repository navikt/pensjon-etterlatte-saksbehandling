import { HistoryEditor } from 'slate-history'
import { BaseEditor } from 'slate'
import { ReactEditor } from 'slate-react'

declare module 'slate' {
  interface CustomTypes {
    Editor: BaseEditor & ReactEditor & HistoryEditor
    Element: CustomElement | CustomHeading | CustomList
    Text: CustomText
  }
}

export type Keybind = 'mod+b' | 'mod+i' | 'mod+u' | 'mod+`'

export type Format = 'bold' | 'italic' | 'underline' | 'code' | 'placeholder'

export const SLATE_HOTKEYS: Record<Keybind, Format> = {
  'mod+b': 'bold',
  'mod+i': 'italic',
  'mod+u': 'underline',
  'mod+`': 'code',
}

export type CustomElement = { [key: string]: any; type: 'paragraph'; align?: string; children: CustomText[] }
export type CustomHeading = { [key: string]: any; type: 'heading'; align?: string; children: CustomText[] }
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
  placeholder?: boolean
}

export type ElementTypes = 'paragraph' | 'heading' | 'list-item' | undefined

export const LIST_TYPES = ['numbered-list', 'bulleted-list']
