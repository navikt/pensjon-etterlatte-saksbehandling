import styled from 'styled-components'
import Spinner from '~shared/Spinner'
import { Box, Heading, HStack, ToggleGroup, VStack } from '@navikt/ds-react'
import { SakMedBehandlinger } from '~components/person/typer'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
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
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { statusErRedigerbar } from '~components/behandling/felles/utils'

export enum OppgaveValg {
  AKTIVE = 'AKTIVE',
  FERDIGSTILTE = 'FERDIGSTILTE',
}

export const SakOversikt = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [oppgaveValg, setOppgaveValg] = useState<OppgaveValg>(OppgaveValg.AKTIVE)
  const [oppgaverResult, oppgaverFetch] = useApiCall(hentOppgaverTilknyttetSak)
  const omgjoerAvslagEnabled = useFeatureEnabledMedDefault('omgjoer-avslag', false)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      oppgaverFetch(sakResult.data.sak.id)
    }
  }, [fnr, sakResult])

  return (
    <>
      {mapResult(sakResult, {
        pending: <Spinner label="Henter sak og behandlinger" />,
        error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
        success: ({ sak, behandlinger }) => (
          <HStack gap="4" wrap={false}>
            <SakHeaderWrapper>
              <SakOversiktHeader sak={sak} behandlinger={behandlinger} fnr={fnr} />
            </SakHeaderWrapper>
            <VStack gap="8">
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
                  success: (oppgaver) => <ForenkletOppgaverTable oppgaver={oppgaver} oppgaveValg={oppgaveValg} />,
                })}
              </VStack>

              <VStack gap="4" align="start">
                <Heading size="medium">Behandlinger</Heading>
                <Behandlingsliste sakOgBehandlinger={{ sak, behandlinger }} />
                {revurderingKanOpprettes(behandlinger, sak.enhet, innloggetSaksbehandler.enheter) && (
                  <OpprettRevurderingModal sakId={sak.id} sakType={sak.sakType} />
                )}
                {omgjoerAvslagEnabled &&
                  omgjoeringAvslagKanOpprettes(behandlinger, sak.enhet, innloggetSaksbehandler.enheter) && (
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

              <VStack gap="4">
                <Heading size="medium">Tilbakekrevinger</Heading>
                <TilbakekrevingListe sakId={sak.id} />
              </VStack>
            </VStack>
          </HStack>
        ),
      })}
    </>
  )
}

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;
`

const SakHeaderWrapper = styled.div`
  padding: var(--a-spacing-8);
  border-right: 1px solid var(--a-surface-active);
  height: 100vh;
  min-width: 25rem;
`
