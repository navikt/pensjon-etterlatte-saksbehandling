import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { Table } from '@navikt/ds-react'

export const AvdoedesBarn = (): ReactNode => {
  return (
    <Personopplysning heading="Avdødes barn" icon={<ChildEyesIcon height="2rem" width="2rem" />}>
      <Table></Table>
    </Personopplysning>
  )
}
