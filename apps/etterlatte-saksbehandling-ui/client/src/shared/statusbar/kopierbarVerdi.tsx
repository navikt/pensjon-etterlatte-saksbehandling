import { CopyToClipboard } from '@navikt/ds-react-internal'

export const KopierbarVerdi = (props: { value: string; copy?: boolean }) => {
  return (
    <CopyToClipboard
      copyText={props.value}
      popoverText={`Kopierte ${props.value}`}
      size={'small'}
      iconPosition={'right'}
    >
      {props.value}
    </CopyToClipboard>
  )
}
