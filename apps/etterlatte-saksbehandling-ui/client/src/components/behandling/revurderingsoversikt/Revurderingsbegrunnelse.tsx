import { Heading, Textarea } from '@navikt/ds-react'
import styled from 'styled-components'

type BegrunnelseProps = {
  begrunnelse: string
  setBegrunnelse: (b: string) => void
  feilmelding?: string | null
  redigerbar: boolean
}

export const Revurderingsbegrunnelse = (props: BegrunnelseProps) => {
  const { begrunnelse, setBegrunnelse, feilmelding, redigerbar } = props
  return redigerbar ? (
    <BegrunnelseWrapper>
      <Heading size="medium" level="3">
        Begrunnelse
      </Heading>
      <Textarea
        value={begrunnelse}
        onChange={(e) => {
          setBegrunnelse(e.target.value)
        }}
        placeholder="Begrunnelse"
        error={feilmelding && !begrunnelse && 'Begrunnelse kan ikke være tom'}
        label=""
      />
    </BegrunnelseWrapper>
  ) : (
    <BegrunnelseWrapper>
      <Heading size="medium" level="3" spacing>
        Begrunnelse
      </Heading>
      {begrunnelse}
    </BegrunnelseWrapper>
  )
}

const BegrunnelseWrapper = styled.div`
  gap: 1rem;
  padding-right: 1rem;
  margin-top: 0rem;
  padding-bottom: 1rem;
`
