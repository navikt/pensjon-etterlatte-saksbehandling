import React, { ReactNode } from 'react'
import styled from 'styled-components'

export const HjelpeTekst = ({ children }: { children: ReactNode | string }): ReactNode => {
  return <HjelpeTekstWrapper>{children}</HjelpeTekstWrapper>
}

const HjelpeTekstWrapper = styled.div`
  display: flex;
  gap: 0.5em;
`
