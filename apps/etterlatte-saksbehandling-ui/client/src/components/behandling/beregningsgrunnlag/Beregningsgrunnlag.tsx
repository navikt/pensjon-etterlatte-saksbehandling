import { SakType } from '~shared/types/sak'
import BeregningsgrunnlagBarnepensjon from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjon'
import BeregningsgrunnlagOmstillingsstoenad from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagOmstillingsstoenad'
import { Box, Heading, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { hentOverstyrBeregning, hentOverstyrBeregningGrunnlag } from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OverstyrBeregning } from '~shared/types/Beregning'
import React, { useEffect, useState } from 'react'
import OverstyrBeregningGrunnlag from './overstyrGrunnlagsBeregning/OverstyrBeregningGrunnlag'
import { Vedtaksresultat } from '~components/behandling/felles/Vedtaksresultat'

import { isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { statusErRedigerbar } from '~components/behandling/felles/utils'
import { SkruPaaOverstyrtBeregning } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/SkruPaaOverstyrtBeregning'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { formaterNavn } from '~shared/types/Person'
import { Personopplysninger } from '~shared/types/grunnlag'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

const Beregningsgrunnlag = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props

  const [overstyrtBeregningGrunnlagResponse, getOverstyrBeregningGrunnlag] = useApiCall(hentOverstyrBeregningGrunnlag)
  const [overstyrtBeregningResponse, getOverstyrtBeregning] = useApiCall(hentOverstyrBeregning)

  const [overstyrtBeregning, setOverstyrtBeregning] = useState<OverstyrBeregning | undefined>(undefined)

  const vedtaksresultat = useVedtaksResultat()

  const visOverstyrKnapp = useFeaturetoggle(FeatureToggle.overstyr_beregning_knapp)

  const [visOverstyrtBeregningGrunnlag, setVisOverstyrtBeregningGrunnlag] = useState<boolean>(false)
  const [erBehandlingFerdigstilt] = useState<boolean>(!statusErRedigerbar(behandling.status))

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterDato(behandling.virkningstidspunkt.dato)
    : undefined

  useEffect(() => {
    if (erBehandlingFerdigstilt) {
      getOverstyrBeregningGrunnlag(behandling.id, (result) => {
        if (result) {
          setVisOverstyrtBeregningGrunnlag(result.perioder.length > 0)
        }
      })
    } else {
      getOverstyrtBeregning(behandling.id, (result) => {
        if (result) {
          setOverstyrtBeregning(result)
          setVisOverstyrtBeregningGrunnlag(true)
        }
      })
    }
  }, [])

  /* For å håndtere første aktivering og deaktivering av overstyrt beregning */
  useEffect(() => {
    setVisOverstyrtBeregningGrunnlag(!erBehandlingFerdigstilt && !!overstyrtBeregning)
  }, [overstyrtBeregning])

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading spacing size="large" level="1">
          Beregningsgrunnlag
        </Heading>
        <Vedtaksresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
      </Box>
      <VStack gap="space-12" paddingInline="space-16">
        {(isSuccess(overstyrtBeregningResponse) || isSuccess(overstyrtBeregningGrunnlagResponse)) && (
          <>
            {visOverstyrKnapp && !erBehandlingFerdigstilt && !overstyrtBeregning && (
              <SkruPaaOverstyrtBeregning behandlingId={behandling.id} setOverstyrt={setOverstyrtBeregning} />
            )}

            {visOverstyrtBeregningGrunnlag && (
              <OverstyrBeregningGrunnlag behandling={behandling} setOverstyrt={setOverstyrtBeregning} />
            )}

            {!visOverstyrtBeregningGrunnlag &&
              {
                [SakType.BARNEPENSJON]: <BeregningsgrunnlagBarnepensjon />,
                [SakType.OMSTILLINGSSTOENAD]: <BeregningsgrunnlagOmstillingsstoenad />,
              }[behandling.sakType]}
          </>
        )}{' '}
        {isFailureHandler({
          apiResult: overstyrtBeregningResponse,
          errorMessage: 'Det oppsto en feil.',
        })}
      </VStack>
    </>
  )
}

export default Beregningsgrunnlag

export const tagTekstForKunEnJuridiskForelder = (behandling: IBehandlingReducer) => {
  const datoTomKunEnJuridiskForelder = behandling?.beregningsGrunnlag?.kunEnJuridiskForelder?.tom

  return datoTomKunEnJuridiskForelder
    ? `Kun én juridisk forelder til og med ${formaterDato(datoTomKunEnJuridiskForelder)}`
    : `Kun én juridisk forelder`
}

export const mapNavn = (fnr: string, personopplysninger: Personopplysninger | null): string => {
  const opplysning = personopplysninger?.avdoede?.find(
    (personOpplysning) => personOpplysning.opplysning.foedselsnummer === fnr
  )?.opplysning

  if (!opplysning) {
    return fnr === 'UKJENT_AVDOED' ? 'Ukjent avdød' : fnr
  }
  return `${formaterNavn(opplysning)} (${fnr})`
}
