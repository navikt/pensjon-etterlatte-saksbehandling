import { Link, useNavigate, useParams } from 'react-router-dom'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgave } from '~shared/api/oppgaver'
import { useContext, useEffect, useState } from 'react'
import { Alert, BodyShort, Button, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentJournalpost } from '~shared/api/dokument'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { ConfigContext } from '~clientConfig'
import { NyttBrevModal } from '~components/person/brev/NyttBrevModal'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { MeldtInnEndringSkjema } from '~components/meldtInnEndring/MeldtInnEndringSkjema'
import { Opprinnelse } from '~shared/types/IDetaljertBehandling'

export enum MeldtInnEndringHandlingValgt {
  AVSLUTT_OPPGAVE,
  OPPRETT_REVURDERING,
  INGEN,
}

export const MeldtInnEndring = () => {
  useSidetittel('Meldt inn endring')

  const { oppgaveId } = useParams()
  const configContext = useContext(ConfigContext)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const navigate = useNavigate()

  if (!oppgaveId) {
    return <Alert variant="error">Oppgave ID ligger ikke med i URL</Alert>
  }

  const [meldtInnEndringHandlingValgt, setMeldtInnEndringHandlingValgt] = useState<MeldtInnEndringHandlingValgt>(
    MeldtInnEndringHandlingValgt.INGEN
  )

  const [hentOppgaveResult, hentOppgaveFetch] = useApiCall(hentOppgave)
  const [hentJournalpostResult, hentJournalpostFetch] = useApiCall(hentJournalpost)

  useEffect(() => {
    hentOppgaveFetch(oppgaveId!, (oppgave) => hentJournalpostFetch(oppgave.referanse!))
  }, [oppgaveId])

  return (
    <>
      {mapResult(hentOppgaveResult, {
        pending: <Spinner label="Henter oppgave..." />,
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
                  <Heading size="large" level="1">
                    Melding om endring
                  </Heading>
                  {meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.INGEN && (
                    <Alert variant="info">
                      <VStack gap="2">
                        <BodyShort>
                          Bruker har meldt inn en endring. Velg mellom å opprette en revurdering eller å avslutte
                          oppgaven dersom den ikke skal behandles.
                        </BodyShort>
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
                  )}
                  {erOppgaveRedigerbar(oppgave.status) &&
                  innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident ? (
                    <>
                      {meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.INGEN ? (
                        <>
                          <Label>Endringen er svar på etteroppgjøret</Label>

                          <div>
                            <Button
                              onClick={() =>
                                navigate(`/svar-paa-etteroppgjoer/${oppgaveId}`, {
                                  state: { opprinnelse: Opprinnelse.MELD_INN_ENDRING_SKJEMA },
                                })
                              }
                            >
                              Behandle mottatt svar etteroppgjør
                            </Button>
                          </div>

                          <Label>Endringen krever en revurdering</Label>
                          <BodyShort>
                            Hvis endringen har betydning for omstillingsstønaden skal det opprettes en revurdering. Når
                            du oppretter en revurdering avsluttes denne oppgaven.
                          </BodyShort>

                          <div>
                            <Button
                              onClick={() =>
                                setMeldtInnEndringHandlingValgt(MeldtInnEndringHandlingValgt.OPPRETT_REVURDERING)
                              }
                            >
                              Opprett revurdering
                            </Button>
                          </div>

                          <Label>Endringen krever ikke revurdering</Label>
                          <BodyShort>
                            Hvis endringen ikke har betydning for omstillingsstønad skal du likevel svare bruker på
                            henvendelsen (
                            <HjemmelLenke
                              lenke="https://lovdata.no/pro/lov/1967-02-10/§11"
                              tittel="forvaltningsloven §11"
                            />
                            ). Velg svarmåte, deretter kan du avslutte oppgaven.
                          </BodyShort>

                          <HStack gap="4">
                            <Button
                              as={Link}
                              variant="secondary"
                              icon={<ExternalLinkIcon aria-hidden />}
                              iconPosition="right"
                              to={`${configContext['modiapersonoversiktUrl']}/person/${oppgave.fnr}`}
                              target="_blank"
                            >
                              Gi beskjed i modia
                            </Button>
                            <NyttBrevModal
                              sakId={oppgave.sakId}
                              sakType={oppgave.sakType}
                              modalButtonlabel="Send brev"
                              modalButtonVariant="secondary"
                            />
                            <Button
                              onClick={() =>
                                setMeldtInnEndringHandlingValgt(MeldtInnEndringHandlingValgt.AVSLUTT_OPPGAVE)
                              }
                            >
                              Avslutt oppgave
                            </Button>
                          </HStack>
                        </>
                      ) : (
                        <MeldtInnEndringSkjema
                          oppgave={oppgave}
                          meldtInnEndringHandlingValgt={meldtInnEndringHandlingValgt}
                          setMeldtInnEndringHandlingValgt={setMeldtInnEndringHandlingValgt}
                        />
                      )}
                    </>
                  ) : (
                    <Alert variant="warning">Du må tildele deg oppgaven for å endre den</Alert>
                  )}
                </VStack>
              </>
            ),
          }),
      })}
    </>
  )
}
