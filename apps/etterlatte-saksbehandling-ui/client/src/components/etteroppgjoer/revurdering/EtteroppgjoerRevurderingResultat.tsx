import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Box, HStack, Label, VStack } from '@navikt/ds-react'
import { EtteroppgjoerResultatType } from '~shared/types/EtteroppgjoerForbehandling'
import { EnvelopeClosedIcon } from '@navikt/aksel-icons'

const ResultatVisning = ({ tekst }: { tekst: string }) => (
  <HStack gap="2" maxWidth="fit-content">
    <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
    <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
      <Label>{tekst}</Label>
    </VStack>
  </HStack>
)

export const EtteroppgjoerRevurderingResultat = () => {
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
        <ResultatVisning tekst="Etteroppgjøret viser at det blir tilbakekreving" />
      )}
      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.ETTERBETALING && (
        <ResultatVisning tekst="Etteroppgjøret viser at det blir etterbetaling" />
      )}
      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER && (
        <ResultatVisning tekst="Etteroppgjøret viser ingen endring" />
      )}
    </Box>
  )
}
