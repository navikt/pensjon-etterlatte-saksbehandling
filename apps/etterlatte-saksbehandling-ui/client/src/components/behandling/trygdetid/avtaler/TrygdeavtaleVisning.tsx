import { BodyShort, Box, Label, VStack } from '@navikt/ds-react'
import { Trygdeavtale, TrygdetidAvtale, TrygdetidAvtaleKriteria } from '~shared/api/trygdetid'
import { JaNeiRec } from '~shared/types/ISvar'

interface TrygdeavtaleVisningProps {
  trygdeavtale: Trygdeavtale
  avtaler: TrygdetidAvtale[]
  kriterier: TrygdetidAvtaleKriteria[]
}

export const TrygdeavtaleVisning = ({ trygdeavtale, avtaler, kriterier }: TrygdeavtaleVisningProps) => {
  const avtale = avtaler.find((avtale) => trygdeavtale.avtaleKode === avtale.kode)
  const avtaleDato = avtale?.datoer.find((dato) => trygdeavtale.avtaleDatoKode === dato.kode)
  const avtaleKriteria = kriterier.find((kriteria) => trygdeavtale.avtaleKriteriaKode === kriteria.kode)

  return (
    <VStack gap="space-8">
      <Box>
        <Label>Avtale</Label>
        <BodyShort>
          {avtale && avtale.beskrivelse}
          {avtaleDato && <> - {avtaleDato.beskrivelse}</>}
        </BodyShort>
      </Box>

      {avtaleKriteria && (
        <Box>
          <Label>Avtalekriterier</Label>
          <BodyShort>{avtaleKriteria.beskrivelse}</BodyShort>
        </Box>
      )}
      {trygdeavtale.personKrets && (
        <Box>
          <Label>Er avdøde i personkretsen i denne avtalen? </Label>
          <BodyShort>{JaNeiRec[trygdeavtale.personKrets]}</BodyShort>
        </Box>
      )}
      {trygdeavtale.arbInntekt1G && (
        <VStack gap="space-2">
          <Box>
            <Label>Er arbeidsinntekt i avtaleland på minst 1 G på dødstidspunktet?</Label>
            <BodyShort>{JaNeiRec[trygdeavtale.arbInntekt1G]}</BodyShort>
          </Box>
          {trygdeavtale.arbInntekt1GKommentar && (
            <Box>
              <Label>Kommentar</Label>
              <BodyShort>{trygdeavtale.arbInntekt1GKommentar}</BodyShort>
            </Box>
          )}
        </VStack>
      )}
      {trygdeavtale.beregArt50 && (
        <VStack gap="space-2">
          <Box>
            <Label>Beregning etter artikkel 50 (EØS-forordning 883/2004)?</Label>
            <BodyShort>{JaNeiRec[trygdeavtale.beregArt50]}</BodyShort>
          </Box>

          {trygdeavtale.beregArt50Kommentar && (
            <Box>
              <Label>Kommentar</Label>
              <BodyShort>{trygdeavtale.beregArt50Kommentar}</BodyShort>
            </Box>
          )}
        </VStack>
      )}

      {trygdeavtale.nordiskTrygdeAvtale && (
        <VStack gap="space-2">
          <Box>
            <Label>Nordisk trygdeavtale: Skal artikkel 9 anvendes - fremtidig trygdetid avkortes?</Label>
            <BodyShort>{JaNeiRec[trygdeavtale.nordiskTrygdeAvtale]}</BodyShort>
          </Box>

          {trygdeavtale.nordiskTrygdeAvtaleKommentar && (
            <Box>
              <Label>Kommentar</Label>
              <BodyShort>{trygdeavtale.nordiskTrygdeAvtaleKommentar}</BodyShort>
            </Box>
          )}
        </VStack>
      )}
    </VStack>
  )
}
