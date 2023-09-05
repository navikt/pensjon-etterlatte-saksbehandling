import { Button, Modal, Heading, BodyShort } from '@navikt/ds-react'
import { useState } from 'react'
import { handlinger } from './typer'
import { fattVedtak as fattVedtakApi } from '~shared/api/behandling'
import { useNavigate, useParams } from 'react-router-dom'
import { ButtonWrapper } from '~shared/modal/modal'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'

export const SendTilAttesteringModal: React.FC = () => {
  const navigate = useNavigate()

  const [isOpen, setIsOpen] = useState(false)
  const { behandlingId } = useParams()

  const [fattVedtakStatus, fattVedtak] = useApiCall(fattVedtakApi)

  const goToOppgavebenken = () => {
    navigate('/')
  }

  const send = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    fattVedtak(behandlingId, () => {
      setIsOpen(false)
      goToOppgavebenken()
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
        className={'padding-modal'}
      >
        <Modal.Body>
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
            <Button
              loading={isPending(fattVedtakStatus)}
              variant="primary"
              size="medium"
              className="button"
              onClick={send}
            >
              {handlinger.ATTESTERING_MODAL.JA.navn}
            </Button>
          </ButtonWrapper>
          {isFailure(fattVedtakStatus) && <ApiErrorAlert>En feil skjedde under attestering av vedtaket.</ApiErrorAlert>}
        </Modal.Body>
      </Modal>
    </>
  )
}
