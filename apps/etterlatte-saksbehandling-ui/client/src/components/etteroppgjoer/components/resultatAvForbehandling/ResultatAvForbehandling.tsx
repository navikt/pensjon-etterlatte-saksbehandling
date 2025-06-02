import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, HStack, Label, VStack } from '@navikt/ds-react'
import { EtteroppgjoerResultatType } from '~shared/types/EtteroppgjoerForbehandling'
import { NOK } from '~utils/formatering/formatering'

export const ResultatAvForbehandling = () => {
  const { beregnetEtteroppgjoerResultat, behandling } = useEtteroppgjoer()
  if (!beregnetEtteroppgjoerResultat) return null

  const { resultatType, differanse } = beregnetEtteroppgjoerResultat
  const absoluttBeloep = Math.abs(differanse)

  const resultatTekst: Record<EtteroppgjoerResultatType, string> = {
    TILBAKEKREVING: 'Tilbakekreving',
    ETTERBETALING: 'Etterbetaling',
    IKKE_ETTEROPPGJOER: 'Ikke etteroppgjør',
  }

  const beskrivelse = (() => {
    if (resultatType === EtteroppgjoerResultatType.TILBAKEKREVING) {
      return `Resultatet viser at det er utbetalt ${NOK(absoluttBeloep)} for mye stønad i ${behandling.aar}. Beløpet blir derfor krevd tilbake.`
    }

    if (resultatType === EtteroppgjoerResultatType.ETTERBETALING) {
      return `Resultatet viser at det er utbetalt ${NOK(absoluttBeloep)} for lite stønad i ${behandling.aar}. Beløpet blir derfor etterbetalt.`
    }

    if (resultatType === EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER) {
      if (differanse > 0) {
        return `Resultatet viser at det er utbetalt ${NOK(absoluttBeloep)} for mye stønad i ${behandling.aar}, men beløpet er innenfor toleransegrensen for tilbakekreving, og det kreves derfor ikke tilbake.`
      }

      if (differanse < 0) {
        return `Resultatet viser at det er utbetalt ${NOK(absoluttBeloep)} for lite stønad i ${behandling.aar}, men beløpet er innenfor toleransegrensen for etterbetaling, og det blir derfor ikke utbetalt.`
      }

      return `Resultatet viser ingen endring, bruker fikk utbetalt rett stønad i ${behandling.aar}.`
    }
  })()

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
      <HStack gap="2" maxWidth="fit-content">
        <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
          <Label>{resultatTekst[resultatType]}</Label>
          <BodyShort>{beskrivelse}</BodyShort>
        </VStack>
      </HStack>
    </Box>
  )
}
