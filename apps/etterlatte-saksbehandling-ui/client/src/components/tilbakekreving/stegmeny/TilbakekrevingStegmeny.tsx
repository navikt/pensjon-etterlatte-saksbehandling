import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import React from 'react'
import { TilbakekrevingNavLenke } from '~components/tilbakekreving/stegmeny/TilbakekrevingNavLenke'

export function TilbakekrevingStegmeny() {
  return (
    <StegMenyWrapper>
      <TilbakekrevingNavLenke path="oversikt" description="Oversikt" enabled={true} />
      <TilbakekrevingNavLenke path="vurdering" description="Vurdering" enabled={true} />
      <TilbakekrevingNavLenke path="vedtak" description="Vedtak" enabled={true} />
      <TilbakekrevingNavLenke path="brev" description="Brev" enabled={true} />
    </StegMenyWrapper>
  )
}
