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
