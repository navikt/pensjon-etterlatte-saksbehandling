import React from 'react'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { OverstyrBeregningsperiode } from '~shared/types/Beregning'
import { BodyShort, Box, HStack, Label } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'

export const OverstyrBeregningsgrunnlagExpandableRowContent = ({
  overtyrBeregningsgrunnlagPeriode,
  sakType,
}: {
  overtyrBeregningsgrunnlagPeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>
  sakType: SakType
}) => {
  return (
    <HStack gap="space-8">
      <div>
        <Label>Anvendt trygdetid (år)</Label>
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
      {sakType == SakType.BARNEPENSJON && (
        <div>
          <Label>Foreldreløssats</Label>
          <BodyShort>{`${overtyrBeregningsgrunnlagPeriode.data.foreldreloessats ? 'Ja' : overtyrBeregningsgrunnlagPeriode.data.foreldreloessats === false ? 'Nei' : 'Ikke besvart'}`}</BodyShort>
        </div>
      )}

      <Box maxWidth="7rem">
        <Label>Beskrivelse</Label>
        <BodyShort>{overtyrBeregningsgrunnlagPeriode.data.beskrivelse}</BodyShort>
      </Box>
    </HStack>
  )
}
