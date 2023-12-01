import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { handlinger } from './typer'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FlexRow } from '~shared/styled'
import { ApiResponse } from '~shared/api/apiClient'
import { hentOppgaveForBehandlingUnderBehandlingIkkeattestert } from '~shared/api/oppgaver'

import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'

export const SendTilAttesteringModal = ({
  behandlingId,
  fattVedtakApi,
  sakId,
  validerKanSendeTilAttestering,
}: {
  behandlingId: string
  fattVedtakApi: (id: string) => Promise<ApiResponse<unknown>>
  sakId: number
  validerKanSendeTilAttestering: () => boolean
}) => {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [fattVedtakStatus, fattVedtak] = useApiCall(fattVedtakApi)
  const [saksbehandlerPaaOppgave, setSaksbehandlerPaaOppgave] = useState<string | null>(null)
  const [oppgaveForBehandlingStatus, requesthentOppgaveForBehandling] = useApiCall(
    hentOppgaveForBehandlingUnderBehandlingIkkeattestert
  )

  useEffect(() => {
    requesthentOppgaveForBehandling({ referanse: behandlingId, sakId: sakId }, (saksbehandler, statusCode) => {
      if (statusCode === 200) {
        setSaksbehandlerPaaOppgave(saksbehandler)
      }
    })
  }, [])

  const fattVedtakWrapper = () => {
    fattVedtak(behandlingId, () => {
      setIsOpen(false)
      navigate('/')
    })
  }

  const klikkAttester = () => {
    if (validerKanSendeTilAttestering()) {
      setIsOpen(true)
    }
  }

  return (
    <>
      {isSuccess(oppgaveForBehandlingStatus) && (
        <>
          {saksbehandlerPaaOppgave ? (
            <>
              <Button variant="primary" onClick={klikkAttester}>
                {handlinger.SEND_TIL_ATTESTERING.navn}
              </Button>
            </>
          ) : (
            <Alert variant="error">
              Oppgaven til denne behandlingen må tildeles en saksbehandler før man kan sende til attestering
            </Alert>
          )}
        </>
      )}
      {isFailure(oppgaveForBehandlingStatus) && (
        <ApiErrorAlert>Fatting er ikke tilgjengelig for øyeblikket. Sjekk oppgavestatus for vedkommende</ApiErrorAlert>
      )}
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
              Nei, avbryt
            </Button>
            <Button loading={isPending(fattVedtakStatus)} variant="primary" onClick={fattVedtakWrapper}>
              Ja, send til attestering
            </Button>
          </FlexRow>
          {isFailure(fattVedtakStatus) && (
            <ApiErrorAlert>
              {fattVedtakStatus.error.detail || 'En feil skjedde under attestering av vedtaket'}
            </ApiErrorAlert>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
