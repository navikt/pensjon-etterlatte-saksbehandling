import { Beregning, OverstyrBeregning } from '~shared/types/Beregning'
import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { behandlingErRedigerbar } from '../../felles/utils'
import React, { Dispatch, SetStateAction, useContext, useEffect, useState } from 'react'
import { CalculatorIcon, PlusIcon } from '@navikt/aksel-icons'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregning,
  oppdaterOverstyrBeregningsGrunnlag,
} from '~store/reducers/BehandlingReducer'
import { hentOverstyrBeregningGrunnlag, opprettEllerEndreBeregning } from '~shared/api/beregning'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { NesteOgTilbake } from '../../handlinger/NesteOgTilbake'
import { BehandlingHandlingKnapper } from '../../handlinger/BehandlingHandlingKnapper'
import { BehandlingRouteContext } from '../../BehandlingRoutes'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../../useInnloggetSaksbehandler'
import { BeregningErOverstyrtAlert } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/BeregningErOverstyrtAlert'
import { SkruAvOverstyrtBeregningModal } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/SkruAvOverstyrtBeregningModal'
import { OverstyrtBeregningsgrunnlagTable } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/OverstyrtBeregningsgrunnlagTable'
import { OverstyrBeregningsgrunnlagPeriodeSkjema } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/OverstyrBeregningsgrunnlagPeriodeSkjema'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

const OverstyrBeregningGrunnlag = (props: {
  behandling: IBehandlingReducer
  setOverstyrt: Dispatch<SetStateAction<OverstyrBeregning | undefined>>
}) => {
  const { behandling, setOverstyrt } = props
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const [visOverstyrBeregningPeriodeSkjema, setVisOverstyrBeregningPeriodeSkjema] = useState<boolean>(false)

  const [overstyrBeregningGrunnlagResult, overstyrBeregningGrunnlagRequest] = useApiCall(hentOverstyrBeregningGrunnlag)
  const [opprettEllerEndreBeregningResult, opprettEllerEndreBeregningRequest] = useApiCall(opprettEllerEndreBeregning)

  const { next } = useContext(BehandlingRouteContext)
  const dispatch = useAppDispatch()

  const onSubmit = () => {
    opprettEllerEndreBeregningRequest(behandling.id, (beregning: Beregning) => {
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
      dispatch(oppdaterBeregning(beregning))
      next()
    })
  }

  useEffect(() => {
    overstyrBeregningGrunnlagRequest(behandling.id, (result) => {
      dispatch(oppdaterOverstyrBeregningsGrunnlag(result))
    })
  }, [])

  return (
    <VStack gap="space-12">
      <BeregningErOverstyrtAlert />
      <VStack gap="space-4">
        <HStack gap="space-2">
          <CalculatorIcon fontSize="1.5rem" aria-hidden />
          <Heading size="small">Beregningsgrunnlag for overstyrt beregning</Heading>
        </HStack>
        <VStack gap="space-4" maxWidth="70rem">
          {mapResult(overstyrBeregningGrunnlagResult, {
            pending: <Spinner label="Henter overstyrt beregning grunnlag..." />,
            error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente grunnlag'}</ApiErrorAlert>,
            success: () => (
              <>
                <OverstyrtBeregningsgrunnlagTable behandling={behandling} />
                {redigerbar && visOverstyrBeregningPeriodeSkjema ? (
                  <OverstyrBeregningsgrunnlagPeriodeSkjema
                    behandling={behandling}
                    paaAvbryt={() => setVisOverstyrBeregningPeriodeSkjema(false)}
                    paaLagre={() => setVisOverstyrBeregningPeriodeSkjema(false)}
                  />
                ) : (
                  <div>
                    <Button
                      size="small"
                      variant="secondary"
                      icon={<PlusIcon aria-hidden />}
                      onClick={() => setVisOverstyrBeregningPeriodeSkjema(true)}
                    >
                      Ny periode
                    </Button>
                  </div>
                )}
              </>
            ),
          })}
        </VStack>
      </VStack>

      <div>
        <SkruAvOverstyrtBeregningModal behandlingId={behandling.id} setOverstyrt={setOverstyrt} />
      </div>

      {isFailureHandler({
        errorMessage: 'Kunne ikke opprette ny beregning',
        apiResult: opprettEllerEndreBeregningResult,
      })}

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="neutral-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button variant="primary" onClick={onSubmit} loading={isPending(opprettEllerEndreBeregningResult)}>
              Beregn
            </Button>
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </VStack>
  )
}

export default OverstyrBeregningGrunnlag
