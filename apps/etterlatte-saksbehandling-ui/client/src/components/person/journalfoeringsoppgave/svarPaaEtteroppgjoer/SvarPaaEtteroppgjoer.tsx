import { Alert, BodyShort, Button, Heading, HStack, Textarea, VStack } from '@navikt/ds-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad, hentOppgave } from '~shared/api/oppgaver'
import { hentJournalpost } from '~shared/api/dokument'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { opprettRevurderingEtteroppgjoer } from '~shared/api/revurdering'
import { OppgaveDTO } from '~shared/types/oppgave'
import { navigerTilPersonOversikt } from '~components/person/lenker/navigerTilPersonOversikt'
import { PersonOversiktFane } from '~components/person/Person'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Opprinnelse } from '~shared/types/IDetaljertBehandling'

export const SvarPaaEtteroppgjoer = () => {
  useSidetittel('Svar på etteroppgjør')

  const { oppgaveId } = useParams()

  const {
    state: { opprinnelse },
  } = useLocation()

  if (!oppgaveId) {
    return <Alert variant="error">Oppgave ID ligger ikke med i URL</Alert>
  }

  const [begrunnelse, setBegrunnelse] = useState<string>('')

  const [hentOppgaveResult, hentOppgaveFetch] = useApiCall(hentOppgave)
  const [hentJournalpostResult, hentJournalpostFetch] = useApiCall(hentJournalpost)

  const [ferdigstillOppgaveResult, ferdigstillOppgaveRequest] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [opprettRevurderingResult, opprettRevurderingRequest] = useApiCall(opprettRevurderingEtteroppgjoer)

  const harEtteroppgjoer = true

  const opprettRevurdering = (oppgave: OppgaveDTO) => {
    opprettRevurderingRequest(
      { sakId: oppgave.sakId, opprinnelse: !!opprinnelse ? opprinnelse : Opprinnelse.UKJENT },
      () => {
        avsluttOppgave(oppgave)
      }
    )
  }

  const avsluttOppgave = (oppgave: OppgaveDTO) => {
    ferdigstillOppgaveRequest({ id: oppgave.id, merknad: begrunnelse }, () => {
      navigerTilPersonOversikt(oppgave.fnr!, PersonOversiktFane.SAKER)
    })
  }

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

                  <Textarea
                    label="Begrunnelse (valgfri)"
                    value={begrunnelse || ''}
                    onChange={(e) => setBegrunnelse(e.target.value)}
                  />

                  {isFailureHandler({
                    apiResult: ferdigstillOppgaveResult,
                    errorMessage: 'Feil under ferdigstilling av oppgave',
                  })}

                  {isFailureHandler({
                    apiResult: opprettRevurderingResult,
                    errorMessage: 'Feil under opprettelse av revurdering',
                  })}
                  <HStack justify="space-between">
                    <Button
                      variant="secondary"
                      onClick={() => navigerTilPersonOversikt(oppgave.fnr!, PersonOversiktFane.SAKER)}
                    >
                      Avbryt
                    </Button>
                    <HStack gap="4">
                      <Button
                        loading={isPending(opprettRevurderingResult) || isPending(ferdigstillOppgaveResult)}
                        onClick={() => opprettRevurdering(oppgave)}
                      >
                        Opprett revurdering
                      </Button>
                      <Button
                        loading={isPending(opprettRevurderingResult) || isPending(ferdigstillOppgaveResult)}
                        onClick={() => avsluttOppgave(oppgave)}
                      >
                        Avslutt oppgave
                      </Button>
                    </HStack>
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
