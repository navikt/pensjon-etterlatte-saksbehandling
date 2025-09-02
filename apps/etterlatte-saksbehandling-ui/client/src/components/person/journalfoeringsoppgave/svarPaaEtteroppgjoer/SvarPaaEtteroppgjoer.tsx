import { Alert, BodyShort, Button, Heading, HStack, Textarea, VStack } from '@navikt/ds-react'
import { Link, useParams } from 'react-router-dom'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgave } from '~shared/api/oppgaver'
import { hentJournalpost } from '~shared/api/dokument'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { useSidetittel } from '~shared/hooks/useSidetittel'

export const SvarPaaEtteroppgjoer = () => {
  useSidetittel('Svar på etteroppgjør')

  const { oppgaveId } = useParams()

  if (!oppgaveId) {
    return <Alert variant="error">Oppgave ID ligger ikke med i URL</Alert>
  }

  const harEtteroppgjoer = true

  const [hentOppgaveResult, hentOppgaveFetch] = useApiCall(hentOppgave)
  const [hentJournalpostResult, hentJournalpostFetch] = useApiCall(hentJournalpost)

  useEffect(() => {
    hentOppgaveFetch(oppgaveId!, (oppgave) => hentJournalpostFetch(oppgave.referanse!))
  }, [oppgaveId])

  return harEtteroppgjoer ? (
    <>
      {mapResult(hentOppgaveResult, {
        pending: <Spinner label="Henter oppgaver..." />,
        error: (error) => <ApiErrorAlert>Kunne ikke hente oppgave, på grunn av feil: {error.detail}</ApiErrorAlert>,
        success: (oppgave) =>
          mapResult(hentJournalpostResult, {
            pending: <Spinner label="Henter journalpost..." />,
            error: (error) => (
              <ApiErrorAlert>Kunne ikke hente journalpost, på grunn av feil: {error.detail}</ApiErrorAlert>
            ),
            success: (journalpost) => (
              <>
                <StatusBar ident={oppgave.fnr} />
                <VStack gap="4" paddingInline="16" paddingBlock="16 4" maxWidth="50rem">
                  <Heading size="medium" spacing>
                    Behandling av svar på etteroppgjøret
                  </Heading>

                  <Alert variant="info">
                    <VStack gap="2">
                      <BodyShort>Bruker har meldt inn svar på etteroppgjøret</BodyShort>

                      <div>
                        <Button
                          as={Link}
                          icon={<ExternalLinkIcon aria-hidden />}
                          size="small"
                          to={`/api/dokumenter/${journalpost.journalpostId}/${journalpost.dokumenter[0].dokumentInfoId}`}
                          target="_blank"
                        >
                          Åpne dokument (åpnes i ny fane)
                        </Button>
                      </div>
                    </VStack>
                  </Alert>

                  <Heading size="small" level="3">
                    Svaret er knyttet til en forbehandling
                  </Heading>
                  <BodyShort>
                    Svaret fra bruker er automatisk knyttet til forbehandlingen bla bla bla ukablad (her kommer det
                    kanskje info om forbehandlingen?)
                  </BodyShort>

                  <BodyShort>
                    Hvis bruker har gitt nok informasjon kan revurderingen for etteroppgjøret opprettes, ellers kan
                    oppgaven avsluttes.
                  </BodyShort>

                  <Textarea label="Begrunnelse (valgfri)" />

                  <HStack gap="4">
                    <Button>Opprett revurdering</Button>
                    <Button>Avslutt oppgave</Button>
                  </HStack>
                </VStack>
              </>
            ),
          }),
      })}
    </>
  ) : (
    <Alert variant="error">Innbygger har ikke etteroppgjør</Alert>
  )
}
