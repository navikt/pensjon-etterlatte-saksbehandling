import { Alert, BodyShort, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { PersongalleriSamsvar } from '~shared/types/grunnlag'
import { formaterKanskjeStringDato } from '~utils/formatering/dato'

export const SoekerErEndretAlert = ({ persongalleriSamsvar }: { persongalleriSamsvar: PersongalleriSamsvar }) => {
  return (
    <Alert variant="warning">
      <VStack gap="4" width="fit-content" maxWidth="42.5rem">
        <BodyShort>
          Søkers identifikator er forskjellig i behandling og PDL. Dette kan f.eks. komme av at bruker har gått fra
          D-nummer til fødselsnummer.
        </BodyShort>

        <HStack gap="4">
          <VStack gap="2">
            <Heading size="xsmall" level="4">
              Ident i behandlingen
            </Heading>

            <VStack width="fit-content">
              <Label size="small">Søker</Label>
              <BodyShort>{persongalleriSamsvar.persongalleri.soeker}</BodyShort>
            </VStack>
            <VStack width="fit-content">
              <Label size="small">Kilde</Label>
              <BodyShort>
                {`${persongalleriSamsvar.kilde?.type.toUpperCase()} (${formaterKanskjeStringDato(persongalleriSamsvar.kilde?.tidspunkt)})`}
              </BodyShort>
            </VStack>
          </VStack>

          <VStack gap="2">
            <Heading size="xsmall" level="4">
              Ident i PDL
            </Heading>

            <VStack width="fit-content">
              <Label size="small">Søker</Label>
              <BodyShort>{persongalleriSamsvar.persongalleriPdl?.soeker ?? 'mangler'}</BodyShort>
            </VStack>
            <VStack width="fit-content">
              <Label size="small">Kilde</Label>
              <BodyShort>{`${persongalleriSamsvar.kildePdl?.type.toUpperCase()} (${formaterKanskjeStringDato(persongalleriSamsvar.kildePdl?.tidspunkt)})`}</BodyShort>
            </VStack>
          </VStack>
        </HStack>
      </VStack>
    </Alert>
  )
}
