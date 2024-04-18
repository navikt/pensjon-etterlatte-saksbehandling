import { Alert, GuidePanel } from '@navikt/ds-react'
import { Container } from '~shared/styled'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const Tilgangsmelding = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  if (!innloggetSaksbehandler.kanSeOppgaveliste) {
    return (
      <Container>
        <GuidePanel>
          Du har lesetilgang til systemet. For å søke opp personer eller saker vennligst benytt søkefelt i hjørnet.
        </GuidePanel>
      </Container>
    )
  } else {
    return (
      <Container>
        <Alert variant="error">Det er ikke registrert lese eller skrivetilgang på deg.</Alert>
      </Container>
    )
  }
}
