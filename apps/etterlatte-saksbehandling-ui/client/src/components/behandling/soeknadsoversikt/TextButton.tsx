import { Collapse } from '@navikt/ds-icons'
import { Expand } from '@navikt/ds-icons'
import { TextButtonWrapper } from './styled'

type Props = {
  isOpen: boolean
  setIsOpen: React.Dispatch<React.SetStateAction<boolean>>
}

export const TextButton: React.FC<Props> = ({ isOpen, setIsOpen }) => {
  return (
    <TextButtonWrapper onClick={() => setIsOpen(!isOpen)}>
      <div className="textButton" onClick={() => setIsOpen(!isOpen)}>
        Historikk {isOpen ? <Collapse className="dropdownIcon" /> : <Expand className="dropdownIcon" />}
      </div>
    </TextButtonWrapper>
  )
}
