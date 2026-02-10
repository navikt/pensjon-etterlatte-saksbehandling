import { Alert, BodyShort, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiResponse } from '~shared/api/apiClient'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { Oppgavestatus } from '~shared/types/oppgave'
import { handlinger } from '~components/behandling/handlinger/typer'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'

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

  const oppgave = useSelectorOppgaveUnderBehandling()

  const soeker = usePersonopplysninger()?.soeker?.opplysning

  const fattVedtakWrapper = () => {
    fattVedtak(behandlingId, () => {
      setIsOpen(false)
      if (soeker?.foedselsnummer) {
        navigate('/person', { state: { fnr: soeker.foedselsnummer } })
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
      {!oppgave?.saksbehandler?.ident ? (
        <Alert size="small" variant="warning">
          Oppgaven til denne behandlingen må tildeles en saksbehandler før den kan sende til attestering
        </Alert>
      ) : oppgave?.saksbehandler?.ident === innloggetSaksbehandler.ident ? (
        oppgave?.status === Oppgavestatus.PAA_VENT ? (
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
          <HStack gap="space-4" justify="center">
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
          </HStack>
          {isFailureHandler({
            apiResult: fattVedtakStatus,
            errorMessage: 'En feil skjedde under attestering av vedtaket',
          })}
        </Modal.Body>
      </Modal>
    </>
  )
}
