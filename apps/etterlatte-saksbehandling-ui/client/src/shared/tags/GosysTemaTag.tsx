import { GosysTema } from '~shared/types/Gosys'
import { Tag } from '@navikt/ds-react'

export const GosysTemaTag = ({ tema }: { tema: GosysTema }) => {
  switch (tema) {
    case 'EYO':
      return (
        <Tag data-color="meta-lime" variant="moderate">
          Omstillingsstønad
        </Tag>
      )
    case 'EYB':
      return (
        <Tag data-color="meta-purple" variant="moderate">
          Barnepensjon
        </Tag>
      )
    case 'PEN':
      return (
        <Tag data-color="info" variant="strong">
          Pensjon
        </Tag>
      )
    default:
      return (
        <Tag data-color="danger" variant="strong">
          Ukjent tema
        </Tag>
      )
  }
}
