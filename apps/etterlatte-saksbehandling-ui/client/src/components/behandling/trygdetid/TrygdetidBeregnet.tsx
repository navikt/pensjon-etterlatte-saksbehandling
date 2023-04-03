import React from 'react'
import { Heading, TextField } from '@navikt/ds-react'
import { FormWrapper } from '~components/behandling/trygdetid/styled'
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
      <FormWrapper>
        <TextField
          label="Sum trygdetid (år)"
          disabled={true}
          size="medium"
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          value={trygdetid.beregnetTrygdetid?.total}
        />
      </FormWrapper>
    </TrygdetidSum>
  )
}

const TrygdetidSum = styled.div`
  padding: 2em 0 4em 0;
`
