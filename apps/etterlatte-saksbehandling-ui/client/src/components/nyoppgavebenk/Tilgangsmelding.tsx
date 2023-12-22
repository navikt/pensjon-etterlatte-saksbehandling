import { useAppSelector } from '~store/Store'
import { Alert, GuidePanel } from '@navikt/ds-react'
import { Container } from '~shared/styled'

export const Tilgangsmelding = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  if (innloggetSaksbehandler.leseTilgang) {
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
