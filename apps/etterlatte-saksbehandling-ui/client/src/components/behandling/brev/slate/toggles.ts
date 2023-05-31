import { Editor, Element as SlateElement, Transforms } from 'slate'
import { CustomElement, ElementTypes, Format, LIST_TYPES } from '~components/behandling/brev/slate/slate-types'

export const toggleBlock = (editor: Editor, format: string) => {
  const isActive = isBlockActive(editor, format)
  const isList = LIST_TYPES.includes(format)

  Transforms.unwrapNodes(editor, {
    match: (n: any) => !Editor.isEditor(n) && SlateElement.isElement(n) && LIST_TYPES.includes(n.type),
    split: true,
  })
  const type = isActive ? 'paragraph' : isList ? 'list-item' : format
  const newProperties: Partial<SlateElement> = { type: type as ElementTypes } // eslint-disable-line
  Transforms.setNodes<SlateElement>(editor, newProperties)

  if (!isActive && isList) {
    const block = <CustomElement>{ type: format, children: [] }
    Transforms.wrapNodes(editor, block)
  }
}

export const toggleMark = (editor: Editor, format: Format) => {
  const isActive = isMarkActive(editor, format)

  if (isActive) {
    Editor.removeMark(editor, format)
  } else {
    Editor.addMark(editor, format, true)
  }
}

export const isBlockActive = (editor: Editor, format: string) => {
  const { selection } = editor
  if (!selection) return false

  const [match] = Array.from(
    Editor.nodes(editor, {
      at: Editor.unhangRange(editor, selection),
      match: (n) => !Editor.isEditor(n) && SlateElement.isElement(n) && n['type'] === format,
    })
  )

  return !!match
}

export const isMarkActive = (editor: Editor, format: string) => {
  const marks: any = Editor.marks(editor)
  return marks ? marks[format] === true : false
}
