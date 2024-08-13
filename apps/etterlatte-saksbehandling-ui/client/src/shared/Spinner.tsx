import { BodyLong, Loader, LoaderProps } from '@navikt/ds-react'
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
      <Loader {...rest} title={label} />
      {label && <BodyLong spacing>{label}</BodyLong>}
    </SpinnerWrap>
  )
}

const SpinnerWrap = styled.div<{ $margin: string }>`
  display: flex;
  justify-content: center;
  margin: ${(props) => props.$margin};
  text-align: center;
`

export default Spinner
