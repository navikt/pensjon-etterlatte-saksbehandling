import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import React from 'react'
import { TilbakekrevingNavLenke } from '~components/tilbakekreving/stegmeny/TilbakekrevingNavLenke'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'

export function TilbakekrevingStegmeny({ behandling }: { behandling: TilbakekrevingBehandling }) {
  return (
    <StegMenyWrapper>
      <TilbakekrevingNavLenke path="vurdering" description="Vurdering" enabled separator />
      <TilbakekrevingNavLenke
        path="brev"
        description="Brev"
        enabled={!!behandling.tilbakekreving.vurdering.konklusjon}
        separator={false}
      />
    </StegMenyWrapper>
  )
}
