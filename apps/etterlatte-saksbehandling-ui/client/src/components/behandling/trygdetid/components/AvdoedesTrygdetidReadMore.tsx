import React from 'react'
import { Box, HStack, ReadMore, VStack } from '@navikt/ds-react'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'

export const AvdoedesTrygdetidReadMore = () => {
  return (
    <ReadMore header="Mer om avdødes trygdetid">
      <VStack gap="space-2">
        <HStack gap="space-2">
          <HjemmelLenke
            tittel="§ 3-5 Trygdetid ved beregning av ytelser"
            lenke="https://lovdata.no/pro/lov/1997-02-28-19/§3-5"
          />
          <HjemmelLenke tittel="§ 3-7 Beregning trygdetid" lenke="https://lovdata.no/pro/lov/1997-02-28-19/§3-7" />
          <HjemmelLenke
            tittel="EØS-forordning 883/2004 artikkel 52"
            lenke="https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_52"
          />
        </HStack>
        <Box maxWidth="42.5rem">
          Faktisk trygdetid kan gis fra avdøde fylte 16 år til dødsfall. Hadde avdøde opptjent pensjonspoeng fra fylte
          67 år til og med 75 år, gis det også et helt års trygdetid for aktuelle poengår. Fremtidig trygdetid kan gis
          fra dødsfallet til og med kalenderåret avdøde hadde blitt 66 år. Trygdetiden beregnes med maks 40 år. Avdødes
          utenlandske trygdetid fra avtaleland skal legges til for alternativ prorata-beregning av ytelsen. Ulike
          avtaler skal ikke beregnes sammen. Hvis avdøde har uføretrygd, skal som hovedregel trygdetid lagt til grunn i
          uføretrygden benyttes.
        </Box>
      </VStack>
    </ReadMore>
  )
}
