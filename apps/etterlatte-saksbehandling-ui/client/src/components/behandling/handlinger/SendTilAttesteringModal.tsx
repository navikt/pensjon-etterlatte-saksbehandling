import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FlexRow } from '~shared/styled'
import { ApiResponse } from '~shared/api/apiClient'
import { isPending, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { Oppgavestatus } from '~shared/types/oppgave'
import { handlinger } from '~components/behandling/handlinger/typer'
import { useOppgaveUnderBehandling } from '~shared/hooks/useOppgaveUnderBehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const SendTilAttesteringModal = ({
  behandlingId,
  fattVedtakApi,
  validerKanSendeTilAttestering,
}: {
  behandlingId: string
  fattVedtakApi: (id: string) => Promise<ApiResponse<unknown>>
  validerKanSendeTilAttestering: () => boolean
}) => {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [fattVedtakStatus, fattVedtak] = useApiCall(fattVedtakApi)

  const [oppgaveResult] = useOppgaveUnderBehandling({ referanse: behandlingId })

  const soeker = usePersonopplysninger()?.soeker?.opplysning

  const fattVedtakWrapper = () => {
    fattVedtak(behandlingId, () => {
      setIsOpen(false)
      if (soeker?.foedselsnummer) {
        navigate(`/person/${soeker.foedselsnummer}`)
      } else {
        navigate('/')
      }
    })
  }

  const klikkAttester = () => {
    if (validerKanSendeTilAttestering()) {
      setIsOpen(true)
    }
  }

  return (
    <>
      {mapApiResult(
        oppgaveResult,
        <Spinner visible={true} label="Henter saksbehandler" />,
        () => (
          <ApiErrorAlert size="small">
            Fatting er ikke tilgjengelig for øyeblikket. Sjekk oppgavestatus for vedkommende
          </ApiErrorAlert>
        ),
        ({ saksbehandler: saksbehandlerPaaOppgave, status }) => {
          return !saksbehandlerPaaOppgave?.ident ? (
            <Alert size="small" variant="warning">
              Oppgaven til denne behandlingen må tildeles en saksbehandler før den kan sende til attestering
            </Alert>
          ) : saksbehandlerPaaOppgave?.ident === innloggetSaksbehandler.ident ? (
            status === Oppgavestatus.PAA_VENT ? (
              <Alert size="small" variant="warning">
                Kan ikke sende oppgave på vent til attestering
              </Alert>
            ) : (
              <Button variant="primary" onClick={klikkAttester}>
                {handlinger.SEND_TIL_ATTESTERING.navn}
              </Button>
            )
          ) : (
            <Alert size="small" variant="warning">
              Oppgaven til denne behandlingen må tildeles deg før du kan sende til attestering
            </Alert>
          )
        }
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
          {isFailureHandler({
            apiResult: fattVedtakStatus,
            errorMessage: 'En feil skjedde under attestering av vedtaket',
          })}
        </Modal.Body>
      </Modal>
    </>
  )
}
