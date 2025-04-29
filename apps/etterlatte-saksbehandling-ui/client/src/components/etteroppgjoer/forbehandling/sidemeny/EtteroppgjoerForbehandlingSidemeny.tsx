import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Sidebar } from '~shared/components/Sidebar'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import React from 'react'
import { NotatPanel } from '~components/behandling/sidemeny/NotatPanel'
import { EtteroppgjoerForbehandlingSidemenyOppsummering } from '~components/etteroppgjoer/forbehandling/sidemeny/EtteroppgjoerForbehandlingSidemenyOppsummering'

export function EtteroppjoerSidemeny() {
  const etteroppgjoer = useEtteroppgjoer()
  return (
    <Sidebar>
      <EtteroppgjoerForbehandlingSidemenyOppsummering />

      <NotatPanel sakId={etteroppgjoer.behandling.sak.id} fnr={etteroppgjoer.behandling.sak.ident} />

      <DokumentlisteLiten fnr={etteroppgjoer.behandling.sak.ident} />
    </Sidebar>
  )
}
