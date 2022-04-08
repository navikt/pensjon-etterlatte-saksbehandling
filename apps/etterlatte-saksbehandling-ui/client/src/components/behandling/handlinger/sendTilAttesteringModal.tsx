import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { Modal } from '../../../shared/modal/modal'
import styled from 'styled-components'
import { handlinger } from './typer'
import { sendTilAttestering } from '../../../shared/api/behandling'
import { useMatch } from 'react-router'
import { useBehandlingRoutes } from '../BehandlingRoutes'

export const BeregningModal: React.FC = () => {
  const { next } = useBehandlingRoutes()

  const [beregneModalisOpen, setBeregneModalisOpen] = useState(false)
  const match = useMatch('/behandling/:behandlingId/*')
  
  const send = () => {
    if(!match?.params.behandlingId) throw new Error("Mangler behandlingsid");
    console.log(match.params.behandlingId)
    const result = sendTilAttestering(match.params.behandlingId);
    console.log(result);
    // if success
    setBeregneModalisOpen(false)
    next();
    // if not, show error message
  }

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
            <Button variant="primary" size="medium" className="button" onClick={send}>
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
