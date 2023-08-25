import { Textarea } from '@navikt/ds-react'
import React from 'react'

type BegrunnelseProps = {
  begrunnelse: string
  setBegrunnelse: (b: string) => void
}

export const Revurderingsbegrunnelse = (props: BegrunnelseProps) => {
  const { begrunnelse, setBegrunnelse } = props
  return (
    <Textarea
      value={begrunnelse}
      onChange={(e) => {
        setBegrunnelse(e.target.value)
      }}
      placeholder="Begrunnelse"
      error={!!begrunnelse && 'Begrunnelse kan ikke være tom'}
      label=""
    />
  )
}
