import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { Heading } from '@navikt/ds-react'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import React from 'react'
import { NotatPanel } from '~components/behandling/sidemeny/NotatPanel'

export function EtteroppjoerSidemeny() {
  const etteroppgjoer = useEtteroppgjoer()
  return (
    <Sidebar>
      <SidebarPanel $border>
        <Heading size="small">Forbehandling etteroppgjør</Heading>
      </SidebarPanel>

      {etteroppgjoer.behandling.status}

      <NotatPanel sakId={etteroppgjoer.behandling.sak.id} fnr={etteroppgjoer.behandling.sak.ident} />

      <DokumentlisteLiten fnr={etteroppgjoer.behandling.sak.ident} />
    </Sidebar>
  )
}
