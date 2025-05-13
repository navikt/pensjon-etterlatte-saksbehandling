import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Box } from '@navikt/ds-react'
import { EtteroppgjoerResultatType } from '~shared/types/EtteroppgjoerForbehandling'
import EtteroppgjoerResultatVisning from '~components/etteroppgjoer/components/EtteroppgjoerResultatVisning'

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
        <EtteroppgjoerResultatVisning tekst="Etteroppgjøret viser at det blir tilbakekreving" />
      )}
      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.ETTERBETALING && (
        <EtteroppgjoerResultatVisning tekst="Etteroppgjøret viser at det blir etterbetaling" />
      )}
      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER && (
        <EtteroppgjoerResultatVisning tekst="Etteroppgjøret viser ingen endring" />
      )}
    </Box>
  )
}
