import { BodyLong, Loader } from '@navikt/ds-react'
import styled from 'styled-components'

interface Props {
  visible: boolean
  label: string
  margin?: string
  variant?: 'neutral' | 'interaction' | 'inverted'
}

const Spinner = ({ visible, label, margin = '3em', variant }: Props) => {
  if (!visible) return null

  return (
    <SpinnerWrap margin={margin}>
      <div className="spinner-overlay">
        <div className="spinner-content">
          <Loader variant={variant} />
          {label && <BodyLong spacing>{label}</BodyLong>}
        </div>
      </div>
    </SpinnerWrap>
  )
}

const SpinnerWrap = styled.div<{ margin: string }>`
  display: flex;
  justify-content: center;
  margin: ${(props) => props.margin};
  text-align: center;
`

export default Spinner
