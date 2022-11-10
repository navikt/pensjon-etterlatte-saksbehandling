import { Button } from '@navikt/ds-react'
import { Modal } from '~shared/modal/modal'
import styled from 'styled-components'

export type Props = {
  tekst: string
  tekstKnappJa: string
  tekstKnappNei: string
  funksjon: () => void
  setModalisOpen: React.Dispatch<React.SetStateAction<boolean>>
}
export const GeneriskModal: React.FC<Props> = ({ tekst, tekstKnappJa, tekstKnappNei, funksjon, setModalisOpen }) => {
  return (
    <Modal
      onClose={() => {
        setModalisOpen(false)
      }}
    >
      <ModalContent>
        <h2>{tekst}</h2>
        <div>
          <Button variant="primary" size="medium" className="button" onClick={funksjon}>
            {tekstKnappJa}
          </Button>
        </div>
        <div>
          <Button
            variant="secondary"
            size="medium"
            className="button"
            onClick={() => {
              setModalisOpen(false)
            }}
          >
            {tekstKnappNei}
          </Button>
        </div>
      </ModalContent>
    </Modal>
  )
}

export const ModalContent = styled.div`
  .button {
    width: fit-content;
    padding: 0.5em 2.5em 0.5em 2.5em;
    margin-top: 1em;
  }

  h2 {
    width: 500px;
  }
`
