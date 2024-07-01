import { CopyButton } from '@navikt/ds-react'

export const KopierbarVerdi = ({ value, iconPosition }: { value: string; iconPosition?: 'left' | 'right' }) => {
  return <CopyButton copyText={value} size="small" text={value} iconPosition={iconPosition} />
}
