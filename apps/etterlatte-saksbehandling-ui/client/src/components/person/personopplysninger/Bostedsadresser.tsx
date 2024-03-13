import React, { ReactNode, useEffect } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { HouseIcon } from '@navikt/aksel-icons'

export const Bostedsadresser = (): ReactNode => {
  return (
    <Personopplysning heading="Bostedsadresser" icon={<HouseIcon height="2rem" width="2rem" />}>
      <h2>Her kommer det masse addresser</h2>
    </Personopplysning>
  )
}
