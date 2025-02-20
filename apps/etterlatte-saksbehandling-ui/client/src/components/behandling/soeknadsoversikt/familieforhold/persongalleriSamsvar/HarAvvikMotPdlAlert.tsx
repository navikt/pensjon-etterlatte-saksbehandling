import { PersongalleriSamsvar } from '~shared/types/grunnlag'
import { SakType } from '~shared/types/sak'
import { Alert, BodyShort, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import { formaterKanskjeStringDato } from '~utils/formatering/dato'

const formaterListeMedIdenter = (identer?: string[]): string => {
  if (!identer || identer.length === 0) {
    return 'Ingen'
  }
  return identer.join(', ')
}

interface Props {
  persongalleriSamsvar: PersongalleriSamsvar
  sakType: SakType
}

export const HarAvvikMotPdlAlert = ({ persongalleriSamsvar, sakType }: Props) => {
  return (
    <Alert variant="warning">
      <VStack gap="4" width="fit-content" maxWidth="42.5rem">
        {sakType === SakType.BARNEPENSJON ? (
          <BodyShort spacing>
            Det er forskjeller mellom familieforholdet i behandlingen og det familieforholdet vi utleder ut i fra PDL.
            Se nøye over og eventuelt korriger persongalleriet ved å redigere.
          </BodyShort>
        ) : (
          <BodyShort spacing>
            Familieforhold må kontrolleres fordi det er avvik mellom registrert informasjon i behandlingen og det som er
            registrert i PDL. Merk at PDL kan ha mangler i informasjon om samboerskap.
          </BodyShort>
        )}

        <VStack gap="2">
          <Heading size="xsmall" level="4">
            Familieforholdet i behandlingen
          </Heading>
          <HStack gap="4">
            <VStack width="fit-content">
              <Label size="small">Avdøde</Label>
              <BodyShort>{formaterListeMedIdenter(persongalleriSamsvar.persongalleri.avdoed)}</BodyShort>
            </VStack>
            {sakType === SakType.BARNEPENSJON && (
              <VStack width="fit-content">
                <Label size="small">Gjenlevende</Label>
                <BodyShort>{formaterListeMedIdenter(persongalleriSamsvar.persongalleri.gjenlevende)}</BodyShort>
              </VStack>
            )}
            <VStack width="fit-content">
              <Label size="small">Kilde</Label>
              <BodyShort>
                {`${persongalleriSamsvar.kilde?.type.toUpperCase()} (${formaterKanskjeStringDato(persongalleriSamsvar.kilde?.tidspunkt)})`}
              </BodyShort>
            </VStack>
          </HStack>
        </VStack>

        <VStack gap="2">
          <Heading size="xsmall" level="4">
            Familieforholdet i PDL
          </Heading>
          <HStack gap="4">
            <VStack width="fit-content">
              <Label size="small">Avøde</Label>
              <BodyShort>{formaterListeMedIdenter(persongalleriSamsvar.persongalleriPdl?.avdoed)}</BodyShort>
            </VStack>
            {sakType === SakType.BARNEPENSJON && (
              <VStack width="fit-content">
                <Label size="small">Gjenlevende</Label>
                <BodyShort>{formaterListeMedIdenter(persongalleriSamsvar.persongalleriPdl?.gjenlevende)}</BodyShort>
              </VStack>
            )}
            <VStack width="fit-content">
              <Label size="small">Kilde</Label>
              <BodyShort>{`${persongalleriSamsvar.kildePdl?.type.toUpperCase()} (${formaterKanskjeStringDato(persongalleriSamsvar.kildePdl?.tidspunkt)})`}</BodyShort>
            </VStack>
          </HStack>
        </VStack>
      </VStack>
    </Alert>
  )
}
