import styled from 'styled-components'
import { Box } from '@navikt/ds-react'

export const GridContainer = styled.div`
  display: flex;
  height: 100%;
  min-height: 100vh;
`

export const Column = styled.div`
  min-width: 30rem;
  &:nth-child(2) {
    flex-grow: 1;
    border-right: 1px solid #c6c2bf;
    border-left: 1px solid #c6c2bf;
    width: 800px;
    min-width: 800px;
  }
  &:last-child {
    width: 320px;
    border-right: none;
  }
`

// TODO: Dette er litt hacky og burde fikses.
//       Tas i sammenheng med utvikling av brevdesigneren.
export const MainContent = styled(Box)`
  width: 100%;
  border-width: 0 1px 0 0;
  border-color: var(--a-border-subtle);
`

export const CollapsibleSidebar = styled.div<{ $collapsed: boolean }>`
  background: var(--a-white);
  border-left: 1px solid var(--a-border-subtle);
  max-height: fit-content;
  min-width: ${(props) => (props.$collapsed ? '50px' : '20%')};
`

export const SidebarContent = styled.div<{ $collapsed: boolean }>`
  display: ${(props) => (props.$collapsed ? 'none' : 'block')};
  position: sticky;
  top: 8rem;
`
export const Scroller = styled.div`
  overflow-y: scroll;
  height: calc(100vh - 182px); // 182 px er høyden av dekoratøren + pdlpersonbar og stegmenyen
  padding-bottom: 2rem;
`

export const SidebarTools = styled.div`
  border-top: 1px solid #c6c2bf;
  background-color: #f8f8f8;

  position: fixed;
  bottom: 0;
  width: 100%;
  z-index: 999;
`
