import { Textarea } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'

type BegrunnelseProps = {
  begrunnelse: string
  setBegrunnelse: (b: string) => void
}

export const Revurderingsbegrunnelse = (props: BegrunnelseProps) => {
  const { begrunnelse, setBegrunnelse } = props
  return (
    <BegrunnelseWrapper>
      <Textarea
        value={begrunnelse}
        onChange={(e) => {
          setBegrunnelse(e.target.value)
        }}
        placeholder="Begrunnelse"
        error={!!begrunnelse && 'Begrunnelse kan ikke være tom'}
        label=""
      />
    </BegrunnelseWrapper>
  )
}

const BegrunnelseWrapper = styled.div`
  gap: 1rem;
  padding-right: 1rem;
  margin-top: 0rem;
  padding-bottom: 1rem;
`
