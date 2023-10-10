import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import React from 'react'
import { TilbakekrevingNavLenke } from '~components/tilbakekreving/stegmeny/TilbakekrevingNavLenke'

export function TilbakekrevingStegmeny() {
  return (
    <StegMenyWrapper>
      <TilbakekrevingNavLenke path="vurdering" description="Vurdering" enabled separator />
      <TilbakekrevingNavLenke path="brev" description="Brev" enabled separator={false} />
    </StegMenyWrapper>
  )
}
