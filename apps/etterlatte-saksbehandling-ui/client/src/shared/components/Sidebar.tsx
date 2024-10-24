import { ReactNode, useState } from 'react'
import { Button } from '@navikt/ds-react'
import styled, { css } from 'styled-components'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'

export const Sidebar = ({ children }: { children: ReactNode }) => {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <CollapsibleSidebar $collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed(!collapsed)}>
          {collapsed ? <ChevronLeftDoubleIcon /> : <ChevronRightDoubleIcon />}
        </Button>
      </SidebarTools>

      <SidebarContent $collapsed={collapsed}>
        <Scroller>{children}</Scroller>
      </SidebarContent>
    </CollapsibleSidebar>
  )
}

export const SidebarPanel = styled.div<{ $border?: boolean }>`
  margin: 20px 8px 0 8px;
  padding: 1em;
  max-width: 800px;

  ${(props) =>
    props.$border
      ? css`
          border-radius: 3px;
          border: 1px solid #c7c0c0;
        `
      : null}
`

const CollapsibleSidebar = styled.div<{ $collapsed: boolean }>`
  background: var(--a-white);
  border-left: 1px solid var(--a-border-subtle);
  min-width: ${(props) => (props.$collapsed ? '50px' : '20%')};
`

const SidebarContent = styled.div<{ $collapsed: boolean }>`
  display: ${(props) => (props.$collapsed ? 'none' : 'block')};
  position: sticky;
  top: 8rem;
`
const Scroller = styled.div`
  overflow-y: scroll;
  height: calc(100vh - 182px); // 182 px er høyden av dekoratøren + pdlpersonbar og stegmenyen
  padding-bottom: 2rem;
`

const SidebarTools = styled.div`
  border-top: 1px solid #c6c2bf;
  background-color: #f8f8f8;

  position: fixed;
  bottom: 0;
  width: 100%;
  z-index: 999;
`
