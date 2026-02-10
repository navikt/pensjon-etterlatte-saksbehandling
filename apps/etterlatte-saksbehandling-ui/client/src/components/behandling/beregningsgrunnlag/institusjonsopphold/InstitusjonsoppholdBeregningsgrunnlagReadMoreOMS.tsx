import React from 'react'
import { BodyLong, ReadMore, VStack } from '@navikt/ds-react'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'

export const InstitusjonsoppholdBeregningsgrunnlagReadMoreOMS = () => {
  return (
    <ReadMore header="Mer om institusjonsopphold">
      <VStack gap="space-2">
        <HjemmelLenke
          tittel="§ 17-13.Ytelser til gjenlevende ektefelle under opphold i institusjon"
          lenke="https://lovdata.no/lov/1997-02-28-19/§17-13"
        />
        <BodyLong>
          Omstillingsstønad kan reduseres som følge av opphold i en institusjon med fri kost og losji under statlig
          ansvar eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske
          sykehusavdelinger. Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for at stønaden skal bli
          redusert. Dersom vedkommende har faste og nødvendige utgifter til bolig, skal stønaden ikke reduseres eller
          reduseres mindre enn hovedregelen sier. Ytelsen skal ikke reduseres når etterlatte forsørger barn.
        </BodyLong>
      </VStack>
    </ReadMore>
  )
}
