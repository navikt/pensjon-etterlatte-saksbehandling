import React, { ReactNode } from 'react'
import { Heading } from '@navikt/ds-react'
import { Container } from '~shared/styled'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'

export const Personopplysninger = (): ReactNode => {
  return (
    <Container>
      <Heading size="large">Her kommer det masse personopplysninger, bare vent du!</Heading>
      <Personopplysning heading="Bostedsadresser">
        <div>OIoaisdghsoidghsoighsoighsoigdhsdoigh oih</div>
      </Personopplysning>
    </Container>
  )
}
