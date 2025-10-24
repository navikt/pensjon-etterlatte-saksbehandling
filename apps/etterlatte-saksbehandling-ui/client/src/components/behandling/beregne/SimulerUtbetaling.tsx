import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSimulertUtbetaling, simulerUtbetaling } from '~shared/api/utbetaling'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import React, { useEffect, useState } from 'react'
import { BodyLong, BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import { SimulertBeregning } from '~shared/types/Utbetaling'
import { formaterDato } from '~utils/formatering/dato'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { useAppSelector } from '~store/Store'
import { SakType } from '~shared/types/sak'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { SimuleringGruppertPaaAar } from './SimuleringGruppertPaaAar'
import { UtbetalingTable } from './UtbetalingTable'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

export const SimulerUtbetaling = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const [simuleringStatus, simulerUtbetalingRequest] = useApiCall(simulerUtbetaling)
  const [lagretSimuleringStatus, hentLagretSimulerUtbetaling] = useApiCall(hentSimulertUtbetaling)
  const [, setLagretSimulerUtbetaling] = useState<SimulertBeregning | null>()

  // For OMS, lytte etter oppdatert beregning/avkorting
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting)

  const erOpphoer = behandling.vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  function behandlingStatusFerdigEllerVedtakFattet() {
    return erFerdigBehandlet(behandling.status) || behandling.status === IBehandlingStatus.FATTET_VEDTAK
  }

  useEffect(() => {
    if (behandlingStatusFerdigEllerVedtakFattet()) {
      hentLagretSimulerUtbetaling(behandling.id, (result, statusCode) => {
        if (statusCode === 200) {
          setLagretSimulerUtbetaling(result)
        }
      })
    } else {
      simuler()
    }
  }, [behandling.status, avkorting])

  const simuler = () => {
    if (
      behandling.status === IBehandlingStatus.BEREGNET ||
      behandling.status === IBehandlingStatus.AVKORTET ||
      erOpphoer
    ) {
      simulerUtbetalingRequest(behandling.id)
    }
  }

  return (
    <>
      <Box paddingBlock="12">
        <Heading spacing size="small" level="2">
          Simulere utbetaling
        </Heading>

        {behandlingStatusFerdigEllerVedtakFattet() &&
          mapResult(lagretSimuleringStatus, {
            pending: <Spinner label="Henter lagret simulering..." />,
            success: (lagretSimulering) =>
              lagretSimulering ? (
                <SimuleringBeregning data={lagretSimulering} />
              ) : (
                <BodyShort textColor="subtle">Fant ingen lagret simulering.</BodyShort>
              ),
            error: () => <ApiErrorAlert>Feil ved henting av lagret simulering</ApiErrorAlert>,
          })}

        {mapResult(simuleringStatus, {
          pending: <Spinner label="Simulerer..." />,
          success: (simuleringrespons) =>
            simuleringrespons ? (
              <SimuleringBeregning data={simuleringrespons} />
            ) : (
              <>
                {behandling.sakType == SakType.OMSTILLINGSSTOENAD && (
                  <BodyLong>
                    Fant ingen utbetalinger å simulere. Dette kan være fordi omstillingsstønaden er avkortet til 0 på
                    grunn av inntekt, sanksjon e.l.
                  </BodyLong>
                )}
                {behandling.sakType == SakType.BARNEPENSJON && (
                  <BodyLong>
                    Fant ingen utbetalinger å simulere. Dette kan være fordi barnepensjonen er avkortet til 0 på grunn
                    av uføretrygd e.l.
                  </BodyLong>
                )}
              </>
            ),
          error: () => <ApiErrorAlert>Feil ved simulering</ApiErrorAlert>,
        })}
      </Box>
    </>
  )
}

const SimuleringBeregning = ({ data }: { data: SimulertBeregning }) => {
  const visGrupperteSimuleringer = useFeaturetoggle(FeatureToggle.ny_simuleringsvisning)
  return (
    <VStack gap="8">
      <UtbetalingTable tittel="Kommende utbetaling(er)" perioder={data.kommendeUtbetalinger} />

      {visGrupperteSimuleringer ? (
        <SimuleringGruppertPaaAar data={data} />
      ) : (
        <>
          {data.etterbetaling.length > 0 && <UtbetalingTable tittel="Simulering" perioder={data.etterbetaling} />}

          {data.tilbakekreving.length > 0 && (
            <UtbetalingTable tittel="Potensiell tilbakekreving" perioder={data.tilbakekreving} />
          )}
        </>
      )}

      <>
        Beregnet dato: {formaterDato(data.datoBeregnet)}
        {data.infomelding && <BodyShort textColor="subtle">{data.infomelding}</BodyShort>}
      </>
    </VStack>
  )
}
