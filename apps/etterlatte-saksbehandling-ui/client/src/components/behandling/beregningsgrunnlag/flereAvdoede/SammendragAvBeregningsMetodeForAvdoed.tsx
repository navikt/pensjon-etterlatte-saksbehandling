import React from 'react'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { BodyShort, Label, VStack } from '@navikt/ds-react'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { lastDayOfMonth } from 'date-fns'

interface Props {
  beregningsMetodeForAvdoed: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined
}

export const SammendragAvBeregningsMetodeForAvdoed = ({ beregningsMetodeForAvdoed }: Props) => {
  return beregningsMetodeForAvdoed ? (
    <VStack gap="space-4">
      <VStack gap="space-2">
        <Label>Metode brukt:</Label>
        <BodyShort>
          {formaterEnumTilLesbarString(beregningsMetodeForAvdoed.data.beregningsMetode.beregningsMetode!)}
        </BodyShort>
      </VStack>
      <VStack gap="space-2">
        <Label>Gyldig for beregning:</Label>
        <BodyShort>
          {formaterDatoMedFallback(beregningsMetodeForAvdoed.fom, '')} -{' '}
          {beregningsMetodeForAvdoed.tom && formaterDatoMedFallback(lastDayOfMonth(beregningsMetodeForAvdoed.tom))}
        </BodyShort>
      </VStack>
      <VStack gap="space-2">
        <Label>Begrunnelse</Label>
        <BodyShort>
          {beregningsMetodeForAvdoed.data.beregningsMetode.begrunnelse
            ? beregningsMetodeForAvdoed.data.beregningsMetode.begrunnelse
            : 'Ikke gitt'}
        </BodyShort>
      </VStack>
    </VStack>
  ) : (
    <Label>Metode er ikke satt for avdoed</Label>
  )
}
