import { Button, Modal, Heading, BodyShort } from '@navikt/ds-react'
import { useState } from 'react'
import { handlinger } from './typer'
import { fattVedtak } from '~shared/api/behandling'
import { useMatch } from 'react-router'
import { useNavigate } from 'react-router-dom'
import { ButtonWrapper } from "~shared/modal/modal";

export const SendTilAttesteringModal: React.FC = () => {
  const navigate = useNavigate()

  const [isOpen, setIsOpen] = useState(false)
  const match = useMatch('/behandling/:behandlingId/*')

  const goToOppgavebenken = () => {
    navigate('/')
  }

  const send = () => {
    if (!match?.params.behandlingId) throw new Error('Mangler behandlingsid')

    fattVedtak(match.params.behandlingId).then((response) => {
      if (response.status === 'ok') {
        setIsOpen(false)
        goToOppgavebenken()
      }
    })
  }

  return (
    <>
      <Button variant="primary" size="medium" className="button" onClick={() => setIsOpen(true)}>
        {handlinger.ATTESTERING.navn}
      </Button>
      <Modal
        open={isOpen}
        onClose={() => {
          setIsOpen(false)
        }}
        aria-labelledby="modal-heading"
        className={"padding-modal"}
      >
        <Modal.Content>
          <Heading spacing level="1" id="modal-heading" size="medium">
            Er du sikker på at du vil sende vedtaket til attestering?
          </Heading>
          <BodyShort spacing>Når du sender til attestering vil vedtaket låses og du får ikke gjort endringer</BodyShort>
          <ButtonWrapper>
            <Button
              variant="secondary"
              size="medium"
              className="button"
              onClick={() => {
                setIsOpen(false)
              }}
            >
              {handlinger.ATTESTERING_MODAL.NEI.navn}
            </Button>
            <Button variant="primary" size="medium" className="button" onClick={send}>
              {handlinger.ATTESTERING_MODAL.JA.navn}
            </Button>
          </ButtonWrapper>
        </Modal.Content>
      </Modal>
    </>
  )
}


