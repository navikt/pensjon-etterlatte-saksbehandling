import { BodyLong, Loader } from '@navikt/ds-react'
import styled from 'styled-components'

const Spinner = ({ visible, label, margin = '3em' }: { visible: boolean; label: string; margin?: string }) => {
  if (!visible) return null

  return (
    <SpinnerWrap margin={margin}>
      <div className={'spinner-overlay'}>
        <div className={'spinner-content'}>
          <Loader />
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
