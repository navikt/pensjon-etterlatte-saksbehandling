import { createEditor } from 'slate'
import { Editable, Slate, useSlate, withReact } from 'slate-react'
import { withHistory } from 'slate-history'
import { useCallback, useState } from 'react'
import { ListIcon } from '@navikt/aksel-icons'
import isHotkey from 'is-hotkey'
import styled from 'styled-components'
import { BodyLong, Heading } from '@navikt/ds-react'
import { SLATE_HOTKEYS } from '~components/behandling/brev/slate/slate-types'
import { isBlockActive, isMarkActive, toggleBlock, toggleMark } from '~components/behandling/brev/slate/toggles'

interface EditorProps {
  value: any
  readonly: boolean
  onChange: (value) => void
}

export default function SlateEditor({ value, onChange, readonly }: EditorProps) {
  const renderElement = useCallback((props) => <Element {...props} />, [])
  const renderLeaf = useCallback((props) => <Leaf {...props} />, [])
  const [editor] = useState(() => withReact(withHistory(createEditor())))

  return (
    <BrevEditor>
      <Slate editor={editor} value={value} onChange={onChange} className={'fuk-yu-tu'}>
        {!readonly && (
          <Toolbar>
            <BlockButton format="heading-two" icon="H2" />
            <BlockButton format="heading-three" icon="H3" />
            <BlockButton format="heading-four" icon="H4" />
            <BlockButton format="bulleted-list" icon={<ListIcon title="a11y-title" fontSize="1.5rem" />} />

            <MarkButton format="placeholder" icon="<P/>" />
          </Toolbar>
        )}

        <SlateEditable
          renderElement={renderElement}
          renderLeaf={renderLeaf}
          readOnly={readonly}
          spellCheck
          autoFocus
          onKeyDown={(event) => {
            for (const hotkey in SLATE_HOTKEYS) {
              if (isHotkey(hotkey, event as any)) {
                event.preventDefault()
                const mark = SLATE_HOTKEYS[hotkey]
                toggleMark(editor, mark)
              }
            }
          }}
        />
      </Slate>
    </BrevEditor>
  )
}

const Element = ({ attributes, children, element }) => {
  const style = { textAlign: element.align }
  switch (element.type) {
    case 'heading-two':
      // noinspection TypeScriptValidateTypes
      return (
        <Heading level="2" size="large" spacing style={style} {...attributes}>
          {children}
        </Heading>
      )
    case 'heading-three':
      // noinspection TypeScriptValidateTypes
      return (
        <Heading level="3" size="medium" spacing style={style} {...attributes}>
          {children}
        </Heading>
      )
    case 'heading-four':
      // noinspection TypeScriptValidateTypes
      return (
        <Heading level="4" size="small" spacing style={style} {...attributes}>
          {children}
        </Heading>
      )
    case 'bulleted-list':
      return (
        <ul style={style} {...attributes}>
          {children}
        </ul>
      )
    case 'list-item':
      return (
        <li style={style} {...attributes}>
          {children}
        </li>
      )
    default:
      // noinspection TypeScriptValidateTypes
      return (
        <BodyLong style={style} spacing {...attributes}>
          {children}
        </BodyLong>
      )
  }
}

const Leaf = ({ attributes, children, leaf }) => {
  if (leaf.placeholder) {
    children = <strong style={{ background: 'orange' }}>{children}</strong>
  }

  return <span {...attributes}>{children}</span>
}

const BlockButton = ({ format, icon }: { format: string; icon: any }) => {
  const editor = useSlate()
  return (
    <StyleButton
      active={isBlockActive(editor, format)}
      onMouseDown={(event) => {
        event.preventDefault()
        toggleBlock(editor, format)
      }}
    >
      {icon}
    </StyleButton>
  )
}

const MarkButton = ({ format, icon }) => {
  const editor = useSlate()
  return (
    <StyleButton
      active={isMarkActive(editor, format)}
      onMouseDown={(event) => {
        event.preventDefault()
        toggleMark(editor, format)
      }}
    >
      {icon}
    </StyleButton>
  )
}

const StyleButton = styled.span<{ reversed?: boolean; active: boolean }>`
  cursor: pointer;
  color: ${(props) => (props.reversed ? (props.active ? 'white' : '#aaa') : props.active ? 'black' : '#ccc')};
`

const Toolbar = styled.div`
  width: 100%;
  position: relative;
  padding: 1px 18px 17px;
  border-bottom: 2px solid #eee;

  & > * {
    display: inline-block;
  }

  & > * + * {
    margin-left: 15px;
  }
`

const SlateEditable = styled(Editable)`
  padding: 10px;
  height: 100%;
  width: 100%;
  min-height: 60vh;
  max-height: 60vh;
  box-shadow: 0 -5px 20px -10px inset lightgray;
  border-bottom: 1px solid #eee;
  overflow: auto scroll;
`

const BrevEditor = styled.div`
  border: 1px solid #eee;
  background-color: #f7f7f7;
`
