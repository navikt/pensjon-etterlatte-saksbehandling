import CopyToClipboard from 'react-copy-to-clipboard'
import { CopyIcon } from '../icons/copyIcon'

export const Fnr = (props: {value: string; copy?: boolean}) => {
  return (
    <div>
      {props.value}{' '}
      {props.copy && (
        <CopyToClipboard text={props.value}>
          <span
            style={{
              verticalAlign: 'middle', cursor: 'pointer', marginLeft: '1em', marginRight: '1em',
            }}
            aria-label="kopier fødselsnummer"
          >
            <CopyIcon/>
          </span>
        </CopyToClipboard>
      )}
    </div>
  )
}
