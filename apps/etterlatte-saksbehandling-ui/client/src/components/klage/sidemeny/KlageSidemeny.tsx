import { useKlage } from '~components/klage/useKlage'
import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import { useState } from 'react'
import { BodyShort, Button } from '@navikt/ds-react'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'
import { SidebarPanel } from '~components/behandling/SideMeny/SideMeny'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'

export function KlageSidemeny() {
  const klage = useKlage()
  const [collapsed, setCollapsed] = useState(false)

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed((collapsed) => !collapsed)}>
          {collapsed ? <ChevronLeftDoubleIcon /> : <ChevronRightDoubleIcon />}
        </Button>
      </SidebarTools>
      <SidebarContent collapsed={collapsed}>
        <SidebarPanel>
          <BodyShort>Her kan vi vise info om klagen, som sakid: {klage?.sak?.id}</BodyShort>
        </SidebarPanel>
      </SidebarContent>
      {klage?.sak.ident ? <Dokumentoversikt fnr={klage.sak.ident} liten /> : null}
    </CollapsibleSidebar>
  )
}
