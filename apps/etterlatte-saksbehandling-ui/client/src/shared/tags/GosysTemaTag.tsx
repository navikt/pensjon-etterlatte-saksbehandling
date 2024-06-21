import { GosysTema } from '~shared/types/Gosys'
import { Tag } from '@navikt/ds-react'
import { Variants } from '~shared/Tags'

export const GosysTemaTag = ({ tema }: { tema: GosysTema }) => {
  switch (tema) {
    case 'EYO':
      return <Tag variant={Variants.ALT2}>Omstillingsstønad</Tag>
    case 'EYB':
      return <Tag variant={Variants.INFO}>Barnepensjon</Tag>
    case 'PEN':
      return <Tag variant={Variants.ALT1}>Pensjon</Tag>
  }
}
