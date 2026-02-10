import React from 'react'
import { SakType } from '~shared/types/sak'
import { Tag } from '@navikt/ds-react'
import { ChildHairEyesIcon, PlantIcon } from '@navikt/aksel-icons'

export const SakTypeTag = ({
  sakType,
  kort,
  size,
}: {
  sakType: SakType
  kort?: boolean
  size?: 'xsmall' | 'small' | 'medium'
}) => {
  switch (sakType) {
    case SakType.BARNEPENSJON:
      return (
        <Tag data-color="meta-lime" variant="moderate" icon={<ChildHairEyesIcon aria-hidden />} size={size}>
          {kort ? 'BP' : 'Barnepensjon'}
        </Tag>
      )
    case SakType.OMSTILLINGSSTOENAD:
      return (
        <Tag data-color="meta-purple" variant="moderate" icon={<PlantIcon aria-hidden />} size={size}>
          {kort ? 'OMS' : 'Omstillingsstønad'}
        </Tag>
      )
  }
}
