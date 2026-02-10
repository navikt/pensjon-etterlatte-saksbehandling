import { Journalpost, Journalstatus } from '~shared/types/Journalpost'
import { Alert, Button, HStack } from '@navikt/ds-react'
import { isPending, mapResult, Result } from '~shared/api/apiUtils'
import React from 'react'
import { SakMedBehandlinger } from '~components/person/typer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettOppgave } from '~shared/api/oppgaver'
import { useNavigate } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ClickEvent, trackClick } from '~utils/analytics'

export const OpprettJournalfoeringsoppgave = ({
  journalpost,
  sakStatus,
}: {
  journalpost: Journalpost
  sakStatus: Result<SakMedBehandlinger>
}) => {
  const navigate = useNavigate()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [oppgaveResult, apiOpprettOppgave] = useApiCall(opprettOppgave)

  const opprettJournalfoeringsoppgave = (sakId: number, journalpostId: string) => {
    trackClick(ClickEvent.OPPRETT_JOURNALFOERINGSOPPGAVE)

    apiOpprettOppgave(
      {
        sakId,
        request: {
          oppgaveType: Oppgavetype.JOURNALFOERING,
          referanse: journalpostId,
          merknad: `Manuelt opprettet oppgave`,
          oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
          saksbehandler: innloggetSaksbehandler.ident,
        },
      },
      (oppgave) => {
        navigate(`/oppgave/${oppgave.id}`)
      }
    )
  }

  if (journalpost.journalstatus === Journalstatus.FEILREGISTRERT)
    return <Alert variant="warning">Kan ikke opprette oppgave for feilregistrert journalpost</Alert>

  return mapResult(sakStatus, {
    pending: <Spinner label="Henter sak" />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Feil ved henting av sak for bruker'}</ApiErrorAlert>,
    success: (sakMedBehandlinger) => (
      <>
        <Alert variant="info">
          Oppgaven vil bli tilknyttet <i>denne</i> journalposten og sakid {sakMedBehandlinger.sak.id}
        </Alert>

        <br />

        <HStack gap="space-4" justify="end">
          <Button variant="tertiary" onClick={() => window.location.reload()}>
            Avslutt
          </Button>

          <Button
            onClick={() => opprettJournalfoeringsoppgave(sakMedBehandlinger.sak.id, journalpost.journalpostId)}
            loading={isPending(oppgaveResult)}
          >
            Opprett oppgave
          </Button>
        </HStack>
      </>
    ),
  })
}
