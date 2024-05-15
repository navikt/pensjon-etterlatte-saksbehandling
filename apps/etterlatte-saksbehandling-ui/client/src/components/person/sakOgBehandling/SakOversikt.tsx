import styled from 'styled-components'
import { Container, SpaceChildren } from '~shared/styled'
import Spinner from '~shared/Spinner'
import { Heading, ToggleGroup } from '@navikt/ds-react'
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

export enum OppgaveValg {
  AKTIVE = 'AKTIVE',
  FERDIGSTILTE = 'FERDIGSTILTE',
}

export const SakOversikt = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }) => {
  const [oppgaveValg, setOppgaveValg] = useState<OppgaveValg>(OppgaveValg.AKTIVE)

  const [oppgaverResult, oppgaverFetch] = useApiCall(hentOppgaverTilknyttetSak)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      oppgaverFetch(sakResult.data.sak.id)
    }
  }, [fnr, sakResult])

  return (
    <>
      {mapResult(sakResult, {
        pending: <Spinner visible label="Henter sak og behandlinger" />,
        error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
        success: ({ sak, behandlinger }) => (
          <SpaceChildren direction="row">
            <SakHeaderWrapper>
              <SakOversiktHeader sak={sak} behandlinger={behandlinger} fnr={fnr} />
            </SakHeaderWrapper>
            <Container>
              <SpaceChildren gap="2rem">
                <SpaceChildren>
                  <Heading size="medium">Oppgaver</Heading>
                  <ToggleGroup
                    defaultValue={OppgaveValg.AKTIVE}
                    onChange={(val) => setOppgaveValg(val as OppgaveValg)}
                    size="small"
                  >
                    <ToggleGroup.Item value={OppgaveValg.AKTIVE}>Aktive</ToggleGroup.Item>
                    <ToggleGroup.Item value={OppgaveValg.FERDIGSTILTE}>Ferdigstilte</ToggleGroup.Item>
                  </ToggleGroup>
                  {mapResult(oppgaverResult, {
                    pending: <Spinner visible label="Henter oppgaver for sak..." />,
                    error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
                    success: (oppgaver) => <ForenkletOppgaverTable oppgaver={oppgaver} oppgaveValg={oppgaveValg} />,
                  })}
                </SpaceChildren>

                <SpaceChildren>
                  <Heading size="medium">Behandlinger</Heading>
                  <Behandlingsliste sakOgBehandlinger={{ sak, behandlinger }} />
                </SpaceChildren>

                <SpaceChildren>
                  <Heading size="medium">Klager</Heading>
                  <KlageListe sakId={sak.id} />
                </SpaceChildren>

                <SpaceChildren>
                  <Heading size="medium">Tilbakekrevinger</Heading>
                  <TilbakekrevingListe sakId={sak.id} />
                </SpaceChildren>
              </SpaceChildren>
            </Container>
          </SpaceChildren>
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
  padding: 2rem;
  border-right: 1px solid var(--a-surface-active);
  height: 100vh;
  min-width: 25rem;
`
