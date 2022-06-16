import { Dokumentliste } from './dokumentliste'
import styled from 'styled-components'

export const Dokumentoversikt = (props: any) => {
  return (
    <OversiktWrapper>
      <h1>Dokumenter</h1>
      <Dokumentliste dokumenter={props.brev} />
    </OversiktWrapper>
  )
}

export const OversiktWrapper = styled.div`
  min-width: 40em;
  max-width: 70%;

  margin: 3em 1em;
  .behandlinger {
    margin-top: 5em;
  }
`
