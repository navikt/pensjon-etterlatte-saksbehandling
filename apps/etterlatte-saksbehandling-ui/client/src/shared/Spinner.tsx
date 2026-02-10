import { BodyLong, HStack, HStackProps, Loader, LoaderProps } from '@navikt/ds-react'

interface Props extends Omit<LoaderProps, 'title'> {
  visible?: boolean // default: true
  label: string
  margin?: HStackProps['margin']
}

const Spinner = ({ visible, label, margin = 'space-12', ...rest }: Props) => {
  if (visible === false) return null

  return (
    <HStack gap="space-4" align="center" justify="center" margin={margin}>
      <Loader {...rest} title={label} />
      {label && <BodyLong>{label}</BodyLong>}
    </HStack>
  )
}

export default Spinner
