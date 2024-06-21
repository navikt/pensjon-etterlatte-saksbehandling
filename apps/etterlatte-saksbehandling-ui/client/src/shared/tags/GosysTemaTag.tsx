import { GosysTema } from '~shared/types/Gosys'
import { Tag } from '@navikt/ds-react'

export const GosysTemaTag = ({ tema }: { tema: GosysTema }) => {
  switch (tema) {
    case 'EYO':
      return <Tag variant="alt2-moderate">Omstillingsstønad</Tag>
    case 'EYB':
      return <Tag variant="alt1-moderate">Barnepensjon</Tag>
    case 'PEN':
      return <Tag variant="info-filled">Pensjon</Tag>
    default:
      return <Tag variant="error-filled">Ukjent tema</Tag>
  }
}
