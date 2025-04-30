import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, HStack, Label, VStack } from '@navikt/ds-react'
import { EtteroppgjoerResultatType } from '~shared/types/Etteroppgjoer'
import { EnvelopeClosedIcon } from '@navikt/aksel-icons'

export const BrevutfallAvForbehandling = () => {
  const { beregnetEtteroppgjoerResultat } = useEtteroppgjoer()

  if (!beregnetEtteroppgjoerResultat) return null

  return (
    <Box
      marginBlock="8 0"
      paddingInline="6"
      paddingBlock="8"
      background="surface-action-subtle"
      borderColor="border-action"
      borderWidth="0 0 0 4"
      maxWidth="42.5rem"
    >
      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.TILBAKEKREVING && (
        <HStack gap="2" maxWidth="fit-content">
          <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Forbehandlingen viser at det blir tilbakekreving</Label>
            <BodyShort>Du skal sende varselbrev.</BodyShort>
          </VStack>
        </HStack>
      )}
      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.ETTERBETALING && (
        <HStack gap="2" maxWidth="fit-content">
          <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Forbehandlingen viser at det blir etterbetaling</Label>
            <BodyShort>Du skal sende varselbrev.</BodyShort>
          </VStack>
        </HStack>
      )}
      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER && (
        <HStack gap="2" maxWidth="fit-content">
          <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Forbehandlingen viser at det blir ingen endring</Label>
            <BodyShort>Du skal sende informasjonsbrev.</BodyShort>
          </VStack>
        </HStack>
      )}
    </Box>
  )
}
