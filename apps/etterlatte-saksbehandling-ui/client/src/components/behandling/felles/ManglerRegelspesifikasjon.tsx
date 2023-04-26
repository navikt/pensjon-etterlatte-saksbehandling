import styled from 'styled-components'
import { ReactNode } from 'react'

export const ManglerRegelspesifikasjon = (props: { children: ReactNode }) => {
  return (
    <Container>
      <ManglerRegelspesifikasjonWrapper>{props.children}</ManglerRegelspesifikasjonWrapper>
      <ManglerRegelspesifikasjonTekst>Mangler regelspesifikasjon</ManglerRegelspesifikasjonTekst>
    </Container>
  )
}

const Container = styled.div`
  position: relative;
`

const ManglerRegelspesifikasjonWrapper = styled.div`
  border-width: 1px;
  border-style: dashed;
  border-color: red;
`

const ManglerRegelspesifikasjonTekst = styled.div`
  position: absolute;
  top: -14px;
  right: 0px;
  color: red;
  font-size: 0.7em;
  font-style: italic;
`
