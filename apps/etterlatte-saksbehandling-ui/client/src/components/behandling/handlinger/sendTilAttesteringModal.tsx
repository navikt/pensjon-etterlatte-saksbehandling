import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { handlinger } from './typer'
import { useNavigate } from 'react-router-dom'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FlexRow } from '~shared/styled'
import { ApiResponse } from '~shared/api/apiClient'

export const SendTilAttesteringModal = ({
  behandlingId,
  fattVedtakApi,
}: {
  behandlingId: string
  fattVedtakApi: (id: string) => Promise<ApiResponse<unknown>>
}) => {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [fattVedtakStatus, fattVedtak] = useApiCall(fattVedtakApi)

  const goToOppgavebenken = () => {
    navigate('/')
  }

  const send = () => {
    fattVedtak(behandlingId, () => {
      setIsOpen(false)
      goToOppgavebenken()
    })
  }

  return (
    <>
      <Button variant="primary" onClick={() => setIsOpen(true)}>
        {handlinger.ATTESTERING.navn}
      </Button>
      <Modal
        open={isOpen}
        onClose={() => {
          setIsOpen(false)
        }}
        aria-labelledby="modal-heading"
        className="padding-modal"
      >
        <Modal.Body>
          <Heading spacing level="1" id="modal-heading" size="medium">
            Er du sikker på at du vil sende vedtaket til attestering?
          </Heading>
          <BodyShort spacing>Når du sender til attestering vil vedtaket låses og du får ikke gjort endringer</BodyShort>
          <FlexRow justify="center">
            <Button
              variant="secondary"
              onClick={() => {
                setIsOpen(false)
              }}
            >
              {handlinger.ATTESTERING_MODAL.NEI.navn}
            </Button>
            <Button loading={isPending(fattVedtakStatus)} variant="primary" onClick={send}>
              {handlinger.ATTESTERING_MODAL.JA.navn}
            </Button>
          </FlexRow>
          {isFailure(fattVedtakStatus) && <ApiErrorAlert>En feil skjedde under attestering av vedtaket.</ApiErrorAlert>}
        </Modal.Body>
      </Modal>
    </>
  )
}
