import styled from 'styled-components'
import { Container, SpaceChildren } from '~shared/styled'
import Spinner from '~shared/Spinner'
import { Alert, Heading } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { SakMedBehandlinger } from '~components/person/typer'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFlyktningStatusForSak } from '~shared/api/sak'
import { SakType } from '~shared/types/sak'
import { hentMigrertYrkesskadeFordel } from '~shared/api/vilkaarsvurdering'
import { Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { SakOversiktHeader } from '~components/person/sakOgBehandling/SakOversiktHeader'
import { SakIkkeFunnet } from '~components/person/sakOgBehandling/SakIkkeFunnet'
import { ForenkletOppgaverTable } from '~components/person/sakOgBehandling/ForenkletOppgaverTable'

const ETTERLATTEREFORM_DATO = '2024-01'

export const SakOversikt = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }) => {
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
    }
  }, [fnr, sakResult])

  return (
    <Container>
      {mapResult(sakResult, {
        pending: <Spinner visible label="Henter sak og behandlinger" />,
        error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
        success: ({ sak }) => (
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

            <div>
              <Heading size="medium" spacing>
                Oppgaver
              </Heading>
              <ForenkletOppgaverTable sakId={sak.id} />
            </div>
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
