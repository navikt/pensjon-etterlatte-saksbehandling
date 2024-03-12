import React, { ReactNode } from 'react'
import { Container, SpaceChildren } from '~shared/styled'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { HouseIcon, PersonIcon } from '@navikt/aksel-icons'

export const Personopplysninger = (): ReactNode => {
  return (
    <Container>
      <SpaceChildren>
        <Personopplysning heading="Bostedsadresser" icon={<HouseIcon height="2rem" width="2rem" />}>
          <div>OIoaisdghsoidghsoighsoighsoigdhsdoigh oih</div>
        </Personopplysning>
        <Personopplysning heading="Foreldre" icon={<PersonIcon height="2rem" width="2rem" />}>
          <div>sodihgsodioihs dgoihdsgoishg soidhg</div>
        </Personopplysning>
        <Personopplysning heading="Søsken (avdødes barn)" icon={<PersonIcon height="2rem" width="2rem" />}>
          <div>psdgjspgoih oih </div>
        </Personopplysning>
      </SpaceChildren>
    </Container>
  )
}
