import styled from 'styled-components'
import { Container, SpaceChildren } from '~shared/styled'
import Spinner from '~shared/Spinner'
import { Alert, Heading, ToggleGroup } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { SakMedBehandlinger } from '~components/person/typer'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFlyktningStatusForSak } from '~shared/api/sak'
import { SakType } from '~shared/types/sak'
import { hentMigrertYrkesskadeFordel } from '~shared/api/vilkaarsvurdering'
import { Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { SakOversiktHeader } from '~components/person/sakOgBehandling/SakOversiktHeader'
import { SakIkkeFunnet } from '~components/person/sakOgBehandling/SakIkkeFunnet'
import { ForenkletOppgaverTable } from '~components/person/sakOgBehandling/ForenkletOppgaverTable'
import { hentOppgaverTilknyttetSak } from '~shared/api/oppgaver'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Behandlingsliste } from '~components/person/sakOgBehandling/Behandlingsliste'

const ETTERLATTEREFORM_DATO = '2024-01'

export enum OppgaveValg {
  AKTIVE = 'AKTIVE',
  FERDIGSTILTE = 'FERDIGSTILTE',
}

export const SakOversikt = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }) => {
  const [oppgaveValg, setOppgaveValg] = useState<OppgaveValg>(OppgaveValg.AKTIVE)

  const [oppgaverResult, oppgaverFetch] = useApiCall(hentOppgaverTilknyttetSak)

  const [flyktningResult, hentFlyktning] = useApiCall(hentFlyktningStatusForSak)
  const [yrkesskadefordelResult, hentYrkesskadefordel] = useApiCall(hentMigrertYrkesskadeFordel)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentFlyktning(sakResult.data.sak.id)

      const migrertBehandling =
        sakResult.data.sak.sakType === SakType.BARNEPENSJON &&
        sakResult.data.behandlinger.find(
          (behandling) =>
            behandling.kilde === Vedtaksloesning.PESYS && behandling.virkningstidspunkt?.dato === ETTERLATTEREFORM_DATO
        )
      if (migrertBehandling) {
        hentYrkesskadefordel(migrertBehandling.id)
      }

      oppgaverFetch(sakResult.data.sak.id)
    }
  }, [fnr, sakResult])

  return (
    <Container>
      {mapResult(sakResult, {
        pending: <Spinner visible label="Henter sak og behandlinger" />,
        error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
        success: ({ sak, behandlinger }) => (
          <SpaceChildren gap="2rem">
            <SakOversiktHeader fnr={fnr} sak={sak} />

            <Skille />

            {mapResult(flyktningResult, {
              success: (data) =>
                !!data?.erFlyktning && (
                  <>
                    <div>
                      <Alert variant="info" size="small">
                        Saken er markert med flyktning i Pesys og første virkningstidspunkt var{' '}
                        {formaterStringDato(data.virkningstidspunkt)}
                      </Alert>
                    </div>

                    <Skille />
                  </>
                ),
            })}

            {mapResult(yrkesskadefordelResult, {
              success: (data) =>
                !!data && (
                  <>
                    <div>
                      <Alert variant="info" size="small">
                        Søker har yrkesskadefordel fra før 01.01.2024 og har rett til stønad til fylte 21 år.
                      </Alert>
                    </div>

                    <Skille />
                  </>
                ),
            })}

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
          </SpaceChildren>
        ),
      })}
    </Container>
  )
}

const Skille = styled.hr`
  border-color: var(--a-surface-active);
  width: 100%;
`

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;

  .details {
    padding: 0.6em;
  }
`
