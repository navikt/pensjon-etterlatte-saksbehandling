import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import { useState } from 'react'
import { BodyShort, Button } from '@navikt/ds-react'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { SidebarPanel } from '~shared/components/Sidebar'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'

export function TilbakekrevingSidemeny() {
  const tilbakekreving = useTilbakekreving()
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
          <BodyShort>Her kan vi vise info om tilbakekrevingen, som sakid: {tilbakekreving?.sak?.id}</BodyShort>
        </SidebarPanel>
      </SidebarContent>
      {tilbakekreving?.sak.ident ? <Dokumentoversikt fnr={tilbakekreving.sak.ident} liten /> : null}
    </CollapsibleSidebar>
  )
}
