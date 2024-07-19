import React from 'react'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { OverstyrBeregningsperiode } from '~shared/types/Beregning'
import { BodyShort, Box, HStack, Label } from '@navikt/ds-react'

export const OverstyrBeregningsgrunnlagExpandableRowContent = ({
  overtyrBeregningsgrunnlagPeriode,
}: {
  overtyrBeregningsgrunnlagPeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>
}) => {
  return (
    <HStack gap="8">
      <div>
        <Label>Anvendt trygdetid</Label>
        <BodyShort>{overtyrBeregningsgrunnlagPeriode.data.trygdetid}</BodyShort>
      </div>
      <div>
        <Label>Trygdetid tilhørende fnr</Label>
        <BodyShort>{overtyrBeregningsgrunnlagPeriode.data.trygdetidForIdent ?? '-'}</BodyShort>
      </div>
      <div>
        <Label>Prorata brøk</Label>
        <BodyShort>{`${overtyrBeregningsgrunnlagPeriode.data.prorataBroekTeller ?? '-'} / ${overtyrBeregningsgrunnlagPeriode.data.prorataBroekNevner ?? '-'}`}</BodyShort>
      </div>
      <Box maxWidth="7rem">
        <Label>Beskrivelse</Label>
        <BodyShort>{overtyrBeregningsgrunnlagPeriode.data.beskrivelse}</BodyShort>
      </Box>
    </HStack>
  )
}
