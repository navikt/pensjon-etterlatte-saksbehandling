import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkorting } from '~shared/api/avkorting'
import React, { useEffect } from 'react'
import { AvkortingInntekt } from '~components/behandling/avkorting/AvkortingInntekt'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import {
  IBehandlingReducer,
  oppdaterAvkorting,
  oppdaterBehandlingsstatus,
  resetAvkorting,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { mapResult } from '~shared/api/apiUtils'
import { Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { Sanksjon } from '~components/behandling/sanksjon/Sanksjon'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { Box, VStack } from '@navikt/ds-react'
import { SimulerUtbetaling } from '~components/behandling/beregne/SimulerUtbetaling'

export const Avkorting = ({
  behandling,
  resetBrevutfallvalidering,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  resetBrevutfallvalidering: () => void
  resetInntektsavkortingValidering: () => void
}) => {
  const dispatch = useAppDispatch()
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting)
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const visSanksjon = useFeatureEnabledMedDefault('sanksjon', false)

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  useEffect(() => {
    if (!avkorting || avkorting.behandlingId !== behandling.id) {
      dispatch(resetAvkorting())
      hentAvkortingRequest(behandling.id, (res) => {
        const avkortingFinnesOgErUnderBehandling = res && redigerbar
        if (avkortingFinnesOgErUnderBehandling) {
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.AVKORTET))
        }
        dispatch(oppdaterAvkorting(res))
      })
    }
  }, [])

  return (
    <Box paddingBlock="8 0">
      <VStack gap="8">
        {mapResult(avkortingStatus, {
          pending: <Spinner visible label="Henter avkorting" />,
          error: <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>,
          success: () => (
            <AvkortingInntekt
              behandling={behandling}
              redigerbar={redigerbar}
              resetInntektsavkortingValidering={resetInntektsavkortingValidering}
            />
          ),
        })}

        {visSanksjon && <Sanksjon behandling={behandling} />}
        {avkorting && <YtelseEtterAvkorting />}
        {avkorting && <SimulerUtbetaling behandling={behandling} />}
        {avkorting && <Brevutfall behandling={behandling} resetBrevutfallvalidering={resetBrevutfallvalidering} />}
      </VStack>
    </Box>
  )
}
