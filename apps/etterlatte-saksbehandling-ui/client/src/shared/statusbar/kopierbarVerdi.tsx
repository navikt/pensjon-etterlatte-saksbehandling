import { CopyButton } from '@navikt/ds-react'

export const KopierbarVerdi = (props: { value: string }) => {
  return <CopyButton copyText={props.value} size={'small'} />
}
