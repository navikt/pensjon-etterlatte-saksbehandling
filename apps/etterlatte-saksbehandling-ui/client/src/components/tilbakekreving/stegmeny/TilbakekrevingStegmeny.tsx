import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import React from 'react'
import { TilbakekrevingNavLenke } from '~components/tilbakekreving/stegmeny/TilbakekrevingNavLenke'

export function TilbakekrevingStegmeny() {
  return (
    <StegMenyWrapper>
      <TilbakekrevingNavLenke path="oversikt" description="Oversikt" enabled separator />
      <TilbakekrevingNavLenke path="vurdering" description="Vurdering" enabled separator />
      <TilbakekrevingNavLenke path="vedtak" description="Vedtak" enabled separator />
      <TilbakekrevingNavLenke path="brev" description="Brev" enabled separator={false} />
    </StegMenyWrapper>
  )
}
