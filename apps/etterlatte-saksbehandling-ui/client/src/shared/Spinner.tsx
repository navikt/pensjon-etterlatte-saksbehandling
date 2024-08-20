import { BodyLong, HStack, Loader, LoaderProps } from '@navikt/ds-react'
import styled from 'styled-components'

interface Props extends Omit<LoaderProps, 'title'> {
  visible?: boolean // default: true
  label: string
  margin?: string
}

const Spinner = ({ visible, label, margin = '3em', ...rest }: Props) => {
  if (visible === false) return null

  return (
    <SpinnerWrap $margin={margin}>
      <HStack gap="4" align="center" justify="center">
        <Loader {...rest} title={label} />
        {label && <BodyLong>{label}</BodyLong>}
      </HStack>
    </SpinnerWrap>
  )
}

const SpinnerWrap = styled.div<{ $margin: string }>`
  margin: ${(props) => props.$margin};
`

export default Spinner
