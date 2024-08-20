import React from 'react'
import { BodyShort, ReadMore, VStack } from '@navikt/ds-react'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'

export const InstitusjonsoppholdBeregningsgrunnlagReadMoreBP = () => {
  return (
    <ReadMore header="Mer om institusjonsopphold">
      <VStack gap="2">
        <HjemmelLenke
          tittel="§ 18-8.Barnepensjon under opphold i institusjon"
          lenke="https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-6#%C2%A718-8"
        />
        <BodyShort>
          Barnepensjonen skal reduseres under opphold i en institusjon med fri kost og losji under statlig ansvar eller
          tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske sykehusavdelinger. Oppholdet
          må vare i tre måneder i tillegg til innleggelsesmåneden for at barnepensjonen skal bli redusert. Dersom barnet
          har faste og nødvendige utgifter til bolig, kan arbeids- og velferdsetaten bestemme at barnepensjonen ikke
          skal reduseres eller reduseres mindre enn hovedregelen sier.
        </BodyShort>
      </VStack>
    </ReadMore>
  )
}
