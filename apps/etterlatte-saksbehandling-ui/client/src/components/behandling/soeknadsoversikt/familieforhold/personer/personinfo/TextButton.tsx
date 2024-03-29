import { ChevronDownIcon, ChevronUpIcon } from '@navikt/aksel-icons'
import { Button } from '@navikt/ds-react'

type Props = {
  isOpen: boolean
  setIsOpen: React.Dispatch<React.SetStateAction<boolean>>
}

export const TextButton = ({ isOpen, setIsOpen }: Props) => {
  return (
    <Button variant="tertiary" onClick={() => setIsOpen(!isOpen)}>
      Historikk {isOpen ? <ChevronUpIcon className="dropdownIcon" /> : <ChevronDownIcon className="dropdownIcon" />}
    </Button>
  )
}
