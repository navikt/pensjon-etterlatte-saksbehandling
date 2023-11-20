import { SidebarPanel } from '~shared/components/Sidebar'
import { Alert, Heading } from '@navikt/ds-react'

export const Attestering = () => {
  //TODO: må matche oppgaven til kravpakke opp mot innlogget bruker
  const oppgaveErTildeltInnloggetBruker = true
  return (
    <SidebarPanel>
      {oppgaveErTildeltInnloggetBruker ? (
        <>
          <Alert variant="info" size="small">
            Kontroller opplysninger og faglige vurderinger gjort under behandling.
          </Alert>
          <br />

          <Heading size="xsmall">Beslutning</Heading>
        </>
      ) : (
        <></>
      )}
    </SidebarPanel>
  )
}
