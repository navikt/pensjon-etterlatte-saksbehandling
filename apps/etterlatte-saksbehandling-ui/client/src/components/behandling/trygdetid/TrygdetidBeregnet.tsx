import React from 'react'
import { Heading } from '@navikt/ds-react'
import { ITrygdetid } from '~shared/api/trygdetid'
import styled from 'styled-components'

type Props = {
  trygdetid: ITrygdetid
  setTrygdetid: (trygdetid: ITrygdetid) => void
}
export const TrygdetidBeregnet: React.FC<Props> = ({ trygdetid }) => {
  return (
    <TrygdetidSum>
      <Heading spacing size="small" level="3">
        Sum faktisk og fremtidig trygdetid
      </Heading>
      <p>{trygdetid.beregnetTrygdetid?.total} år</p>
    </TrygdetidSum>
  )
}

const TrygdetidSum = styled.div`
  padding: 2em 0 4em 0;
`
