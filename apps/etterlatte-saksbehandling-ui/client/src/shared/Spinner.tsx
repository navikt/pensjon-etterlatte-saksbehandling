import { BodyLong, Loader } from '@navikt/ds-react'
import styled from 'styled-components'

const Spinner = ({ visible, label }: { visible: boolean; label: string }) => {
  if (!visible) return null

  return (
    <SpinnerWrap>
      <div className={'spinner-overlay'}>
        <div className={'spinner-content'}>
          <Loader />
          <BodyLong spacing>{label}</BodyLong>
        </div>
      </div>
    </SpinnerWrap>
  )
}

const SpinnerWrap = styled.div`
  display: flex;
  justify-content: center;
  margin: 3em;
  text-align: center;
`

export default Spinner
