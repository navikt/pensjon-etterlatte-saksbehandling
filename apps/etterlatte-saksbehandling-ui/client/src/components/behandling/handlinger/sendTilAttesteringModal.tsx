import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { Modal } from '../../../shared/modal/modal'
import styled from 'styled-components'
import { handlinger } from './typer'

interface Props {
  nextPage: () => void
}
export const BeregningModal: React.FC<Props> = ({ nextPage }) => {
  const [beregneModalisOpen, setBeregneModalisOpen] = useState(false)
  //TODO send til attestering: EY-500
  return (
    <>
      <Button variant="primary" size="medium" className="button" onClick={() => setBeregneModalisOpen(true)}>
        {handlinger.ATTESTERING.navn}
      </Button>
      {beregneModalisOpen && (
        <Modal
          onClose={() => {
            setBeregneModalisOpen(false)
          }}
        >
          <ModalContent>
            <h2>Er du sikker på at du vil sende vedtaket til attestering?</h2>
            <p>Når du sender til attestering vil vedtaket låses og du får ikke gjort endringer</p>
            <Button
              variant="secondary"
              size="medium"
              className="button"
              onClick={() => {
                setBeregneModalisOpen(false)
              }}
            >
              {handlinger.ATTESTERING_MODAL.NEI.navn}
            </Button>
            <Button variant="primary" size="medium" className="button" onClick={nextPage}>
              {handlinger.ATTESTERING_MODAL.JA.navn}
            </Button>
          </ModalContent>
        </Modal>
      )}
    </>
  )
}

export const ModalContent = styled.div`
  .button {
    width: fit-content;
    padding: 0.7em 2em 0.7em 2em;
    margin-top: 1em;
  }
`
