import { Alert, Box, GuidePanel } from '@navikt/ds-react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const Tilgangsmelding = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  if (!innloggetSaksbehandler.kanSeOppgaveliste) {
    return (
      <Box padding="space-8">
        <GuidePanel>
          Du har lesetilgang til systemet. For å søke opp personer eller saker vennligst benytt søkefelt i hjørnet.
        </GuidePanel>
      </Box>
    )
  } else {
    return (
      <Box padding="space-8">
        <Alert variant="error">Det er ikke registrert lese eller skrivetilgang på deg.</Alert>
      </Box>
    )
  }
}
