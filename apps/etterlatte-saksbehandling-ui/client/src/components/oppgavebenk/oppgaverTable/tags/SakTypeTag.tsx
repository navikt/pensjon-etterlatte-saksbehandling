import React from 'react'
import { SakType } from '~shared/types/sak'
import { Tag } from '@navikt/ds-react'
import { ChildEyesIcon, PlantIcon } from '@navikt/aksel-icons'

export const SakTypeTag = ({ sakType }: { sakType: SakType }) => {
  switch (sakType) {
    case SakType.BARNEPENSJON:
      return (
        <Tag variant="alt2-moderate" icon={<ChildEyesIcon aria-hidden />}>
          BP
        </Tag>
      )
    case SakType.OMSTILLINGSSTOENAD:
      return (
        <Tag variant="alt1-moderate" icon={<PlantIcon aria-hidden />}>
          OMS
        </Tag>
      )
  }
}
