import styled from 'styled-components'

export const Container = styled.div`
  padding: 2rem;
`

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
export const MainContent = styled.div`
  width: 100%;
  min-width: 280px;
  border-left: 1px solid #c6c2bf;
  border-right: 1px solid #c6c2bf;
`

export const CollapsibleSidebar = styled.div<{ collapsed: boolean }>`
  position: relative;
  max-height: fit-content;
  min-width: ${(props) => (props.collapsed ? '50px' : '20%')};
`

export const SidebarContent = styled.div<{ collapsed: boolean }>`
  display: ${(props) => (props.collapsed ? 'none' : 'block')};
  margin-bottom: 4rem;
`

export const SidebarTools = styled.div`
  border-top: 1px solid #c6c2bf;
  background-color: #f8f8f8;

  position: fixed;
  bottom: 0;
  width: 100%;
`

export const Content = styled.div`
  min-height: 70vh;
  margin: 0;
  padding: 0;
`

export const ContentHeader = styled.div`
  padding: 1em 4em;
`

export const Header = styled.div`
  padding: 1em 2em;
`
