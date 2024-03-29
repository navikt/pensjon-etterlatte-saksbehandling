import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { handlinger } from './typer'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FlexRow } from '~shared/styled'
import { ApiResponse } from '~shared/api/apiClient'
import { hentOppgaveForBehandlingUnderBehandlingIkkeattestert } from '~shared/api/oppgaver'
import { isPending, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useAppSelector } from '~store/Store'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

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
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [fattVedtakStatus, fattVedtak] = useApiCall(fattVedtakApi)

  const [oppgaveForBehandlingStatus, requesthentOppgaveForBehandling] = useApiCall(
    hentOppgaveForBehandlingUnderBehandlingIkkeattestert
  )

  const soeker = usePersonopplysninger()?.soeker?.opplysning

  useEffect(() => {
    requesthentOppgaveForBehandling({ referanse: behandlingId, sakId: sakId })
  }, [])

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
        oppgaveForBehandlingStatus,
        <Spinner visible={true} label="Henter saksbehandler" />,
        () => (
          <ApiErrorAlert size="small">
            Fatting er ikke tilgjengelig for øyeblikket. Sjekk oppgavestatus for vedkommende
          </ApiErrorAlert>
        ),
        (saksbehandlerPaaOppgave) => {
          return !saksbehandlerPaaOppgave?.ident ? (
            <Alert size="small" variant="warning">
              Oppgaven til denne behandlingen må tildeles en saksbehandler før den kan sende til attestering
            </Alert>
          ) : saksbehandlerPaaOppgave?.ident === innloggetSaksbehandler.ident ? (
            <Button variant="primary" onClick={klikkAttester}>
              {handlinger.SEND_TIL_ATTESTERING.navn}
            </Button>
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
