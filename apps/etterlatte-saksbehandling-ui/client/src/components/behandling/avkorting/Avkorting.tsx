import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkorting } from '~shared/api/avkorting'
import React, { useEffect } from 'react'
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
import { virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { mapResult } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { Sanksjon } from '~components/behandling/sanksjon/Sanksjon'
import { Alert, BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { IAvkorting } from '~shared/types/IAvkorting'
import { aarFraDatoString } from '~utils/formatering/dato'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { hentBehandlingstatus } from '~shared/api/behandling'
import { AvkortingInntekt } from '~components/behandling/avkorting/AvkortingInntekt'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

export const Avkorting = ({
  behandling,
  resetInntektsavkortingValidering,
  skalHaInntektNesteAar,
}: {
  behandling: IBehandlingReducer
  resetInntektsavkortingValidering: () => void
  skalHaInntektNesteAar: boolean
}) => {
  const dispatch = useAppDispatch()
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting) as IAvkorting
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const [, hentBehandlingstatusRequest] = useApiCall(hentBehandlingstatus)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const visSanksjon = useFeaturetoggle(FeatureToggle.sanksjon)

  const harInstitusjonsopphold = behandling?.beregning?.beregningsperioder.find((bp) => bp.institusjonsopphold)

  const redigerbar =
    behandlingErRedigerbar(behandling.status, behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter) &&
    behandling.revurderingsaarsak != Revurderingaarsak.ETTEROPPGJOER

  useEffect(() => {
    if (!avkorting) {
      dispatch(resetAvkorting())
      hentAvkortingRequest(behandling.id, (avkorting) => {
        hentBehandlingstatusRequest(behandling.id, (status) => {
          dispatch(oppdaterBehandlingsstatus(status))
          dispatch(oppdaterAvkorting(avkorting))
        })
      })
    }
  }, [])

  const avkortingGrunnlagInnevaerendeAar = () => {
    return avkorting?.avkortingGrunnlag.find(
      (grunnlag) => aarFraDatoString(grunnlag.fom) == aarFraDatoString(virkningstidspunkt(behandling).dato)
    )
  }

  const avkortetYtelse = [...(avkorting?.avkortetYtelse ?? [])]
  const tidligereAvkortetYtelse = [...(avkorting?.tidligereAvkortetYtelse ?? [])]

  return (
    <Box paddingBlock="8 0">
      <VStack gap="8">
        {mapResult(avkortingStatus, {
          pending: <Spinner label="Henter avkorting" />,
          error: (e) => <ApiErrorAlert>En feil har oppstått: {e.detail}</ApiErrorAlert>,
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
                avkorting={avkorting}
                skalHaInntektNesteAar={skalHaInntektNesteAar}
                redigerbar={redigerbar}
                resetInntektsavkortingValidering={resetInntektsavkortingValidering}
              />
            </>
          ),
        })}

        {visSanksjon && (
          <Sanksjon behandling={behandling} manglerInntektVirkAar={!avkortingGrunnlagInnevaerendeAar()} />
        )}
        {avkorting && (
          <YtelseEtterAvkorting avkortetYtelse={avkortetYtelse} tidligereAvkortetYtelse={tidligereAvkortetYtelse} />
        )}
      </VStack>
    </Box>
  )
}
