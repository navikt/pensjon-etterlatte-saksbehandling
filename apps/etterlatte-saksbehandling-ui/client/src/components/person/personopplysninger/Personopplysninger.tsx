import React, { ReactNode } from 'react'
import { Container, SpaceChildren } from '~shared/styled'
import { LenkeTilAndreSystemer } from '~components/person/personopplysninger/LenkeTilAndreSystemer'

export const Personopplysninger = (): ReactNode => {
  return (
    <Container>
      <SpaceChildren>
        <LenkeTilAndreSystemer />
      </SpaceChildren>
    </Container>
  )
}
