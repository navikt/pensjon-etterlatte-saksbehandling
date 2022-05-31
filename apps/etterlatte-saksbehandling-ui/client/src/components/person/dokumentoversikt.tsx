import { Dokumentliste } from './dokumentliste'
import styled from 'styled-components'

export const Dokumentoversikt = (props: any) => {
  return (
    <OversiktWrapper>
      <h1>Dokumenter</h1>
      <Dokumentliste {...props} />
    </OversiktWrapper>
  )
}

export const OversiktWrapper = styled.div`
  margin: 3em 1em;
  .behandlinger {
    margin-top: 5em;
  }
`
