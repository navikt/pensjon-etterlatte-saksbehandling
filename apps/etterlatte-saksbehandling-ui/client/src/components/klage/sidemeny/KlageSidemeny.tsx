import { useKlage } from '~components/klage/useKlage'
import { BodyShort } from '@navikt/ds-react'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'

export function KlageSidemeny() {
  const klage = useKlage()

  return (
    <Sidebar>
      <SidebarPanel border>
        <BodyShort>Her kan vi vise info om klagen, som sakid: {klage?.sak?.id}</BodyShort>
      </SidebarPanel>
      {klage?.sak.ident && <Dokumentoversikt fnr={klage.sak.ident} liten />}
    </Sidebar>
  )
}
