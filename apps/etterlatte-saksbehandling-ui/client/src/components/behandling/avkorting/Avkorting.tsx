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
import { IBehandlingStatus, IBehandlingsType, virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { mapResult } from '~shared/api/apiUtils'
import { Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { Sanksjon } from '~components/behandling/sanksjon/Sanksjon'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { Alert, BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import { SimulerUtbetaling } from '~components/behandling/beregne/SimulerUtbetaling'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { IAvkorting } from '~shared/types/IAvkorting'
import { aarFraDatoString } from '~utils/formatering/dato'

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
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting) as IAvkorting
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const visSanksjon = useFeatureEnabledMedDefault('sanksjon', false)

  const toggleAvkortinGrunnlagNesteAar = useFeatureEnabledMedDefault('oms-aarsinntekt-for-to-aar', false)
  console.log(toggleAvkortinGrunnlagNesteAar)

  const harInstitusjonsopphold = behandling?.beregning?.beregningsperioder.find((bp) => bp.institusjonsopphold)

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  useEffect(() => {
    if (!avkorting) {
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

  const avkortingGrunnlagInnevaerendeAar = () => {
    return avkorting?.avkortingGrunnlag.find(
      (grunnlag) => grunnlag.aar == aarFraDatoString(virkningstidspunkt(behandling).dato)
    )
  }

  return (
    <Box paddingBlock="8 0">
      <VStack gap="8">
        {mapResult(avkortingStatus, {
          pending: <Spinner label="Henter avkorting" />,
          error: <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>,
          success: () => (
            <>
              <VStack maxWidth="70rem">
                <Heading spacing size="small" level="2">
                  Inntektsavkorting
                </Heading>
                {harInstitusjonsopphold && (
                  <Alert variant="error">
                    Obs! Det er registrert institusjonsopphold i beregningen og dette er ikke støttet sammen med
                    inntektsavkorting, bruk manuel overstyring.
                  </Alert>
                )}
                <HjemmelLenke tittel="Folketrygdloven § 17-9" lenke="https://lovdata.no/pro/lov/1997-02-28-19/§17-9" />
                <BodyShort spacing>
                  Omstillingsstønaden reduseres med 45 prosent av den gjenlevende sin inntekt som på årsbasis overstiger
                  et halvt grunnbeløp. Inntekt rundes ned til nærmeste tusen. Det er forventet årsinntekt for hvert
                  kalenderår som skal legges til grunn.
                </BodyShort>
                <BodyShort>
                  I innvilgelsesåret skal inntekt opptjent før innvilgelse trekkes fra, og resterende forventet inntekt
                  fordeles på gjenværende måneder. På samme måte skal inntekt etter opphør holdes utenfor i opphørsåret.
                </BodyShort>
              </VStack>
              <AvkortingInntekt
                behandling={behandling}
                avkortingGrunnlagFrontend={avkortingGrunnlagInnevaerendeAar()}
                innevaerendeAar={true}
                redigerbar={redigerbar}
                resetInntektsavkortingValidering={resetInntektsavkortingValidering}
              />{' '}
              {toggleAvkortinGrunnlagNesteAar &&
                behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING && (
                  <AvkortingInntekt
                    behandling={behandling}
                    avkortingGrunnlagFrontend={avkorting?.avkortingGrunnlag[1]}
                    innevaerendeAar={false}
                    redigerbar={redigerbar}
                    resetInntektsavkortingValidering={resetInntektsavkortingValidering}
                  />
                )}
            </>
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
