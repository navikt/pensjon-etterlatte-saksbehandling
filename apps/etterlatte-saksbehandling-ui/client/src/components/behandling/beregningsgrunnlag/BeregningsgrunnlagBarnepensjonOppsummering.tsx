import { Box, Heading } from '@navikt/ds-react'
import { BeregningsgrunnlagMetodeForAvdoedOppsummering } from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagMetodeForAvdoed'
import React from 'react'
import { ITrygdetid } from '~shared/api/trygdetid'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsmetodeForAvdoed } from '~shared/types/Beregning'

type BeregningsgrunnlagBarnepensjonOppsummeringProps = {
  trygdetider: ITrygdetid[]
  mapNavn: (fnr: string) => string
  periodisertBeregningsmetodeForAvdoed: (
    ident: string
  ) => PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | null
}
const BeregningsgrunnlagBarnepensjonOppsummering = (props: BeregningsgrunnlagBarnepensjonOppsummeringProps) => {
  const { trygdetider, mapNavn, periodisertBeregningsmetodeForAvdoed } = props

  return (
    <>
      <Heading size="medium" level="2">
        Oppsummering
      </Heading>

      {trygdetider.map((trygdetid) => {
        const grunnlag = periodisertBeregningsmetodeForAvdoed(trygdetid.ident)
        const navn = mapNavn(trygdetid.ident)

        if (grunnlag !== null) {
          return (
            <BeregningsgrunnlagMetodeForAvdoedOppsummering
              key={`oppsummering-${trygdetid.ident}`}
              beregningsMetode={grunnlag.data.beregningsMetode.beregningsMetode}
              fom={grunnlag.fom}
              tom={grunnlag.tom}
              begrunnelse={grunnlag.data.beregningsMetode.begrunnelse ?? ''}
              navn={navn}
              visNavn={true}
            />
          )
        } else {
          return (
            <Box paddingBlock="4 0" key={`oppsummering-${trygdetid.ident}`}>
              Trygdetid brukt i beregningen for {navn} mangler
            </Box>
          )
        }
      })}
    </>
  )
}

export default BeregningsgrunnlagBarnepensjonOppsummering
