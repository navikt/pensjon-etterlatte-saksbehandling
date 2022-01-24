import { Collapse } from '@navikt/ds-icons'
import { Expand } from '@navikt/ds-icons'
import { TextButtonWrapper } from './styled'

type Props = {
  isOpen: boolean
  setIsOpen: React.Dispatch<React.SetStateAction<boolean>>
  antall: number
}

export const TextButton: React.FC<Props> = ({ isOpen, setIsOpen, antall }) => {
  const getButtonText = () => {
    return antall === 1 ? 'endring' : 'endringer'
  }

  return (
    <TextButtonWrapper onClick={() => setIsOpen(!isOpen)}>
      <div className="textButton" onClick={() => setIsOpen(!isOpen)}>
        {isOpen ? (
          <>
            Skjul {antall} {getButtonText()} <Collapse className="dropdownIcon" />
          </>
        ) : (
          <>
            Vis {antall} {getButtonText()} <Expand className="dropdownIcon" />
          </>
        )}
      </div>
    </TextButtonWrapper>
  )
}
