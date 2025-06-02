import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, HStack, Label, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { EtteroppgjoerResultatType } from '~shared/types/EtteroppgjoerForbehandling'

export const ResultatAvForbehandling = () => {
  const { beregnetEtteroppgjoerResultat, behandling } = useEtteroppgjoer()

  if (!beregnetEtteroppgjoerResultat) return null

  const resultatTekst: Record<EtteroppgjoerResultatType, string> = {
    TILBAKEKREVING: 'Tilbakekreving',
    ETTERBETALING: 'Etterbetaling',
    IKKE_ETTEROPPGJOER: 'Ikke etteroppgjør',
  }

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
          <Label>{resultatTekst[beregnetEtteroppgjoerResultat.resultatType]}</Label>
          <BodyShort>
            {(() => {
              const diff = beregnetEtteroppgjoerResultat.differanse
              const diffBeloep = NOK(Math.abs(diff))
              if (diff > 0) {
                if (diff <= beregnetEtteroppgjoerResultat.grense.tilbakekreving) {
                  return `Resultatet viser at det er utbetalt ${diffBeloep} for mye stønad i ${behandling.aar}, men beløpet er innenfor toleransegrense for tilbakekreving, og det kreves derfor ikke tilbake.`
                }
                return `Resultatet viser at det er utbetalt ${diffBeloep} for mye stønad i ${behandling.aar}. Beløpet blir derfor krevd tilbake.`
              }

              if (diff < 0) {
                if (diff >= -beregnetEtteroppgjoerResultat.grense.etterbetaling) {
                  return `Resultatet viser at det er utbetalt ${diffBeloep} for lite stønad i${behandling.aar}, men beløpet er innenfor toleransegrense for etterbetaling, og det blir derfor ikke utbetalt.`
                }
                return `Resultatet viser at det er utbetalt ${diffBeloep} for lite stønad i ${behandling.aar}. Beløpet blir derfor etterbetalt.`
              }

              return `Resultatet viser ingen endring, bruker fikk utbetalt rett stønad i ${behandling.aar}.`
            })()}
          </BodyShort>
        </VStack>
      </HStack>
    </Box>
  )
}
