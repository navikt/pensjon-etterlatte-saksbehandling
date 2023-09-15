import React from 'react'
import { Heading } from '@navikt/ds-react'
import { ITrygdetid } from '~shared/api/trygdetid'
import styled from 'styled-components'

type Props = {
  trygdetid: ITrygdetid
}
export const TrygdetidBeregnet: React.FC<Props> = ({ trygdetid }) => {
  const beregnetTrygdetid = trygdetid.beregnetTrygdetid
    ? `${trygdetid.beregnetTrygdetid?.resultat.samletTrygdetidNorge} år`
    : '-'

  return (
    <TrygdetidSum>
      <Heading spacing size="small" level="3">
        Sum faktisk og fremtidig trygdetid
      </Heading>
      {beregnetTrygdetid}
    </TrygdetidSum>
  )
}

const TrygdetidSum = styled.div`
  padding: 2em 0 4em 0;
`
