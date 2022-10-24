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
  min-width: 280px;
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

export const Sidebar = styled.div`
  width: 20%;
  min-width: 20%;
  max-width: 20%;
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

export const WarningText = styled.span`
  color: #ba3a26;
  display: block;
`
