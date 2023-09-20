import { ReactNode, useState } from 'react'
import { Button } from '@navikt/ds-react'
import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import styled, { css } from 'styled-components'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'

export const Sidebar = ({ children }: { children: ReactNode }) => {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed(!collapsed)}>
          {collapsed ? <ChevronLeftDoubleIcon /> : <ChevronRightDoubleIcon />}
        </Button>
      </SidebarTools>

      <SidebarContent collapsed={collapsed}>{children}</SidebarContent>
    </CollapsibleSidebar>
  )
}

export const SidebarPanel = styled.div<{ border?: boolean }>`
  margin: 20px 8px 0 8px;
  padding: 1em;

  ${(props) =>
    props.border
      ? css`
          border-radius: 3px;
          border: 1px solid #c7c0c0;
        `
      : null}

  .flex {
    display: flex;
    justify-content: space-between;
  }

  .info {
    margin-top: 1em;
    margin-bottom: 1em;
  }
`
