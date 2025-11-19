import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { Sidebar } from '~shared/components/Sidebar'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import React from 'react'
import { NotatPanel } from '~components/behandling/sidemeny/NotatPanel'
import { EtteroppgjoerForbehandlingSidemenyOppsummering } from '~components/etteroppgjoer/forbehandling/sidemeny/EtteroppgjoerForbehandlingSidemenyOppsummering'
import AnnullerForbehandling from '~components/etteroppgjoer/components/AnnullerForbehandling'

export function EtteroppjoerSidemeny() {
  const { forbehandling } = useEtteroppgjoerForbehandling()
  return (
    <Sidebar>
      <EtteroppgjoerForbehandlingSidemenyOppsummering />

      <NotatPanel sakId={forbehandling.sak.id} fnr={forbehandling.sak.ident} />

      <DokumentlisteLiten fnr={forbehandling.sak.ident} />

      <AnnullerForbehandling />
    </Sidebar>
  )
}
