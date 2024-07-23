import { Beregning, OverstyrBeregning } from '~shared/types/Beregning'
import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { behandlingErRedigerbar } from '../../felles/utils'
import { FEIL_I_PERIODE } from '../PeriodisertBeregningsgrunnlag'
import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
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
import { useBehandlingRoutes } from '../../BehandlingRoutes'
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

  const { next } = useBehandlingRoutes()
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
    <VStack gap="12">
      <BeregningErOverstyrtAlert />
      <VStack gap="4">
        <HStack gap="2">
          <CalculatorIcon fontSize="1.5rem" aria-hidden />
          <Heading size="small">Beregningsgrunnlag for overstyrt beregning</Heading>
        </HStack>
        <VStack gap="4" maxWidth="70rem">
          {mapResult(overstyrBeregningGrunnlagResult, {
            pending: <Spinner visible label="Henter overstyrt beregning grunnlag..." />,
            error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente grunnlag'}</ApiErrorAlert>,
            success: () => (
              <>
                <OverstyrtBeregningsgrunnlagTable />
                {redigerbar && visOverstyrBeregningPeriodeSkjema ? (
                  <OverstyrBeregningsgrunnlagPeriodeSkjema
                    behandling={behandling}
                    setVisOverstyrBeregningPeriodeSkjema={setVisOverstyrBeregningPeriodeSkjema}
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

        <HStack gap="4" align="center">
          <SkruAvOverstyrtBeregningModal behandlingId={behandling.id} setOverstyrt={setOverstyrt} />
        </HStack>
      </VStack>

      {isFailureHandler({
        errorMessage: 'Kunne ikke opprette ny beregning',
        apiResult: opprettEllerEndreBeregningResult,
      })}

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
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

type FeilIPeriodeOverstyrBeregning = (typeof FEIL_I_PERIODE)[number]
export type FeilIPeriodeGrunnlagAlle =
  | FeilIPeriodeOverstyrBeregning
  | 'BELOEP_MANGLER'
  | 'TRYGDETID_MANGLER'
  | 'TRYGDETID_MANGLER_FNR'
  | 'BESKRIVELSE_MANGLER'
  | 'PRORATA_MANGLER'

export const teksterFeilIPeriode: Record<FeilIPeriodeGrunnlagAlle, string> = {
  INGEN_PERIODER: 'Minst en periode må finnes',
  DEKKER_IKKE_SLUTT_AV_INTERVALL: 'Periodene må være komplette tilbake til virk',
  DEKKER_IKKE_START_AV_INTERVALL: 'Periodene må vare ut ytelsen',
  HULL_ETTER_PERIODE: 'Det er et hull i periodene etter denne perioden',
  PERIODE_OVERLAPPER_MED_NESTE: 'Perioden overlapper med neste periode',
  TOM_FOER_FOM: 'Til og med kan ikke være før fra og med',
  BELOEP_MANGLER: 'Utbetalt beløp er påkrevd',
  TRYGDETID_MANGLER: 'Trygdetid er påkrevd',
  TRYGDETID_MANGLER_FNR: 'Trygdetid tilhører FNR er påkrevd',
  BESKRIVELSE_MANGLER: 'Beskrivelse er påkrevd',
  PRORATA_MANGLER: 'Prorata brøk må ha begge felter fyllt ut hvis det er i bruk',
} as const

export default OverstyrBeregningGrunnlag
