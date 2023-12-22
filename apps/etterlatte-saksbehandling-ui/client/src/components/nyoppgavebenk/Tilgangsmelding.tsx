import { useAppSelector } from '~store/Store'
import { Alert, GuidePanel } from '@navikt/ds-react'

export const Tilgangsmelding = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  if (innloggetSaksbehandler.leseTilgang) {
    return (
      <GuidePanel>
        Vi ser at du har lesetilgang til systemet. For å søke opp personer eller saker vennligst benytt søkefelt i
        hjørnet.
      </GuidePanel>
    )
  } else {
    return <Alert variant="error">Det er ikke registrert lese eller skrivetilgang på deg.</Alert>
  }
}
