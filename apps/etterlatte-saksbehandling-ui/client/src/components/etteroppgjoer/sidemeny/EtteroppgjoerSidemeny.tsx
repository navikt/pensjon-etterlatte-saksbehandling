import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Sidebar } from '~shared/components/Sidebar'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import React from 'react'
import { NotatPanel } from '~components/behandling/sidemeny/NotatPanel'
import { EtteroppgjoerSidemenyOppsummering } from '~components/etteroppgjoer/sidemeny/EtteroppgjoerSidemenyOppsummering'

export function EtteroppjoerSidemeny() {
  const etteroppgjoer = useEtteroppgjoer()
  return (
    <Sidebar>
      <EtteroppgjoerSidemenyOppsummering />

      <NotatPanel sakId={etteroppgjoer.behandling.sak.id} fnr={etteroppgjoer.behandling.sak.ident} />

      <DokumentlisteLiten fnr={etteroppgjoer.behandling.sak.ident} />
    </Sidebar>
  )
}
