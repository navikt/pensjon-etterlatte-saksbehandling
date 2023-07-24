import styled from 'styled-components'
import { ChevronUpIcon, ChevronDownIcon } from '@navikt/aksel-icons'
type Props = {
  isOpen: boolean
  setIsOpen: React.Dispatch<React.SetStateAction<boolean>>
}

export const TextButton: React.FC<Props> = ({ isOpen, setIsOpen }) => {
  return (
    <TextButtonWrapper onClick={() => setIsOpen(!isOpen)}>
      <div className="textButton" onClick={() => setIsOpen(!isOpen)}>
        Historikk {isOpen ? <ChevronUpIcon className="dropdownIcon" /> : <ChevronDownIcon className="dropdownIcon" />}
      </div>
    </TextButtonWrapper>
  )
}

export const TextButtonWrapper = styled.div`
  .textButton {
    display: inline-flex;
    justify-content: space-between;
    color: #0067c5;
    :hover {
      cursor: pointer;
    }
    .dropdownIcon {
      margin-bottom: 0;
      margin-left: 0.5em;
      margin-top: 0.1em;
    }
  }
`
