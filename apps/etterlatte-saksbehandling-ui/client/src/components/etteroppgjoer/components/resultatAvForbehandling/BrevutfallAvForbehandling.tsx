import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Box } from '@navikt/ds-react'
import { EtteroppgjoerBehandlingStatus, EtteroppgjoerResultatType } from '~shared/types/EtteroppgjoerForbehandling'
import EtteroppgjoerResultatVisning from '~components/etteroppgjoer/components/EtteroppgjoerResultatVisning'

export const BrevutfallAvForbehandling = () => {
  const { beregnetEtteroppgjoerResultat, behandling } = useEtteroppgjoer()

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
        <EtteroppgjoerResultatVisning
          tekst="Forbehandlingen viser at det blir tilbakekreving"
          body={
            behandling.status === EtteroppgjoerBehandlingStatus.FERDIGSTILT
              ? 'Det skal ha blitt sendt varselbrev'
              : 'Du skal sende varselbrev.'
          }
        />
      )}

      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.ETTERBETALING && (
        <EtteroppgjoerResultatVisning
          tekst="Forbehandlingen viser at det blir etterbetaling"
          body={
            behandling.status === EtteroppgjoerBehandlingStatus.FERDIGSTILT
              ? 'Det skal ha blitt sendt varselbrev'
              : 'Du skal sende varselbrev.'
          }
        />
      )}

      {beregnetEtteroppgjoerResultat.resultatType === EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER && (
        <EtteroppgjoerResultatVisning
          tekst="Forbehandlingen viser at det blir ingen endring"
          body={
            behandling.status === EtteroppgjoerBehandlingStatus.FERDIGSTILT
              ? 'Det skal ha blitt sendt informasjonsbrev'
              : 'Du skal sende informasjonsbrev.'
          }
        />
      )}
    </Box>
  )
}
