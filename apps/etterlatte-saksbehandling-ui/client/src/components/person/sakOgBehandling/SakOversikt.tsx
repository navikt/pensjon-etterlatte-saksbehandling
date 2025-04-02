import Spinner from '~shared/Spinner'
import { Alert, BodyShort, Box, Button, Heading, HStack, ToggleGroup, VStack } from '@navikt/ds-react'
import { SakMedBehandlingerOgKanskjeAnnenSak } from '~components/person/typer'
import { isPending, isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { SakOversiktHeader } from '~components/person/sakOgBehandling/SakOversiktHeader'
import { SakIkkeFunnet } from '~components/person/sakOgBehandling/SakIkkeFunnet'
import { ForenkletOppgaverTable } from '~components/person/sakOgBehandling/ForenkletOppgaverTable'
import { hentOppgaverTilknyttetSak } from '~shared/api/oppgaver'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Behandlingsliste } from '~components/person/sakOgBehandling/Behandlingsliste'
import { KlageListe } from '~components/person/sakOgBehandling/KlageListe'
import { TilbakekrevingListe } from '~components/person/sakOgBehandling/TilbakekrevingListe'
import { omgjoeringAvslagKanOpprettes, revurderingKanOpprettes } from '~components/person/hendelser/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { OpprettRevurderingModal } from '~components/person/OpprettRevurderingModal'
import { OmgjoerAvslagModal } from '~components/person/sakOgBehandling/OmgjoerAvslagModal'
import { statusErRedigerbar } from '~components/behandling/felles/utils'
import { hentGosysOppgaverForPerson } from '~shared/api/gosys'
import { ForenkletGosysOppgaverTable } from '~components/person/sakOgBehandling/ForenkletGosysOppgaverTable'
import { OpprettOppfoelgingsoppgaveModal } from '~components/person/sakOgBehandling/OpprettOppfoelgingsoppgaveModal'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { opprettEtteroppgjoerIDev } from '~shared/api/etteroppgjoer'
import { usePerson } from '~shared/statusbar/usePerson'
import { OppdaterIdentModal } from '~components/person/hendelser/OppdaterIdentModal'
import { EtteroppgjoerForbehandlingListe } from '~components/person/sakOgBehandling/EtteroppgjoerForbehandlingListe'

export enum OppgaveValg {
  AKTIVE = 'AKTIVE',
  FERDIGSTILTE = 'FERDIGSTILTE',
}

function ByttTilAnnenSak(props: { byttSak: () => void }) {
  return (
    <Box>
      <Alert variant="info">
        Bruker har en annen sak.
        <div>
          <Button onClick={props.byttSak}>Se annen sak</Button>
        </div>
      </Alert>
    </Box>
  )
}

export const SakOversikt = ({
  sakResult,
  fnr,
  setForetrukketSak,
}: {
  sakResult: Result<SakMedBehandlingerOgKanskjeAnnenSak>
  fnr: string
  setForetrukketSak: (sakId: number) => void
}) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const etteroppgjoerEnabled = useFeaturetoggle(FeatureToggle.etteroppgjoer)
  const byttTilAnnenSakEnabled = useFeaturetoggle(FeatureToggle.bytt_til_annen_sak)
  const [oppgaveValg, setOppgaveValg] = useState<OppgaveValg>(OppgaveValg.AKTIVE)
  const [oppgaverResult, oppgaverFetch] = useApiCall(hentOppgaverTilknyttetSak)
  const [gosysOppgaverResult, gosysOppgaverFetch] = useApiCall(hentGosysOppgaverForPerson)
  const [opprettEtteroppgjoerStatus, apiOpprettEtteroppjoer] = useApiCall(opprettEtteroppgjoerIDev)

  const person = usePerson()

  const harEndretFnr = () => {
    if (isSuccess(sakResult)) {
      if (person && person.foedselsnummer !== sakResult.data.sak.ident) {
        return true
      }
    }
    return false
  }

  useEffect(() => {
    if (isSuccess(sakResult)) {
      oppgaverFetch(sakResult.data.sak.id)
      gosysOppgaverFetch(fnr)
    }
  }, [fnr, sakResult])

  return (
    <>
      {mapResult(sakResult, {
        pending: <Spinner label="Henter sak og behandlinger" />,
        error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
        success: ({ sak, behandlinger, annenSak }) => (
          <HStack gap="4" wrap={false}>
            <Box padding="8" minWidth="25rem" borderWidth="0 1 0 0" borderColor="border-subtle">
              <SakOversiktHeader sak={sak} behandlinger={behandlinger} fnr={fnr} />
              {byttTilAnnenSakEnabled && annenSak && (
                <ByttTilAnnenSak byttSak={() => setForetrukketSak(annenSak.sak.id)} />
              )}
            </Box>
            <VStack gap="8">
              {harEndretFnr() && (
                <Box paddingBlock="8 0">
                  <Alert variant="info">
                    <Heading size="xsmall" spacing={true}>
                      Nytt identnummer på bruker
                    </Heading>
                    <BodyShort>
                      {fnr !== person!.foedselsnummer
                        ? `Identitetsnummer du søkte på “${fnr}” er blitt ersattet med “${person!.foedselsnummer}”. Du må oppdatere til ny ident.`
                        : `Identitetsnummer er blitt ersattet med “${person!.foedselsnummer}” men sak har fortsatt ident ${sak.ident}. Du må oppdatere til ny ident.`}
                    </BodyShort>
                    <Box paddingBlock="6 2">
                      <OppdaterIdentModal sak={sak} hendelse={null} />
                    </Box>
                  </Alert>
                </Box>
              )}
              <VStack gap="4">
                <Box paddingBlock="8 0">
                  <Heading size="medium">Oppgaver</Heading>
                </Box>
                <ToggleGroup
                  defaultValue={OppgaveValg.AKTIVE}
                  onChange={(val) => setOppgaveValg(val as OppgaveValg)}
                  size="small"
                >
                  <ToggleGroup.Item value={OppgaveValg.AKTIVE}>Aktive</ToggleGroup.Item>
                  <ToggleGroup.Item value={OppgaveValg.FERDIGSTILTE}>Ferdigstilte</ToggleGroup.Item>
                </ToggleGroup>
                {mapResult(oppgaverResult, {
                  pending: <Spinner label="Henter oppgaver for sak..." />,
                  error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
                  success: (oppgaver) => (
                    <ForenkletOppgaverTable
                      oppgaver={oppgaver}
                      oppgaveValg={oppgaveValg}
                      refreshOppgaver={() => oppgaverFetch(sak.id)}
                    />
                  ),
                })}
                <OpprettOppfoelgingsoppgaveModal sak={sak} vedOpprettelse={() => oppgaverFetch(sak.id)} />
              </VStack>

              <VStack gap="4">
                <Box paddingBlock="8 0">
                  <Heading size="medium">Gosys-oppgaver</Heading>
                </Box>

                {mapResult(gosysOppgaverResult, {
                  pending: <Spinner label="Henter oppgaver fra Gosys" />,
                  error: (error) => (
                    <ApiErrorAlert>
                      Feil oppsto ved henting av Gosys-oppgaver <br />
                      {error.detail}
                    </ApiErrorAlert>
                  ),
                  success: (oppgaver) => (
                    <ForenkletGosysOppgaverTable oppgaver={oppgaver} oppgaveValg={OppgaveValg.AKTIVE} />
                  ),
                })}
              </VStack>

              <VStack gap="4" align="start">
                <Heading size="medium">Behandlinger</Heading>
                <Behandlingsliste sakOgBehandlinger={{ sak, behandlinger }} />
                {revurderingKanOpprettes(behandlinger, sak.enhet, innloggetSaksbehandler.enheter) && (
                  <OpprettRevurderingModal sakId={sak.id} sakType={sak.sakType} />
                )}
                {omgjoeringAvslagKanOpprettes(behandlinger, sak.enhet, innloggetSaksbehandler.enheter) && (
                  <OmgjoerAvslagModal
                    sakId={sak.id}
                    harAapenBehandling={behandlinger.some((behandling) => statusErRedigerbar(behandling.status))}
                  />
                )}
              </VStack>

              <VStack gap="4">
                <Heading size="medium">Klager</Heading>
                <KlageListe sakId={sak.id} />
              </VStack>

              <VStack marginBlock="0" gap="4">
                <Heading size="medium">Tilbakekrevinger</Heading>
                <TilbakekrevingListe sakId={sak.id} />
              </VStack>

              {etteroppgjoerEnabled && (
                <VStack marginBlock="0" gap="4">
                  <Heading size="medium">Etteroppgjør forbehandlinger</Heading>
                  <EtteroppgjoerForbehandlingListe sakId={sak.id} />
                  <Box>
                    <Button
                      loading={isPending(opprettEtteroppgjoerStatus)}
                      variant="secondary"
                      onClick={() => apiOpprettEtteroppjoer(sak.id)}
                    >
                      Opprett etteroppgjør
                    </Button>
                  </Box>
                </VStack>
              )}
            </VStack>
          </HStack>
        ),
      })}
    </>
  )
}
