import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import React from 'react'
import { TilbakekrevingNavLenke } from '~components/tilbakekreving/stegmeny/TilbakekrevingNavLenke'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { TilbakekrevingBehandling, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'

export function TilbakekrevingStegmeny() {
  const tilbakekrevingBehandling = useTilbakekreving()

  return (
    <StegMenyWrapper>
      <TilbakekrevingNavLenke path="vurdering" description="Vurdering" enabled separator />
      <TilbakekrevingNavLenke path="utbetalinger" description="Utbetalinger" enabled separator />
      <TilbakekrevingNavLenke path="oppsummering" description="Oppsummering" enabled separator />
      <TilbakekrevingNavLenke
        path="brev"
        description="Brev"
        enabled={kanSeBrev(tilbakekrevingBehandling)}
        separator={false}
      />
    </StegMenyWrapper>
  )
}

function kanSeBrev(tilbakekrevingBehandling: TilbakekrevingBehandling | null): boolean {
  return (
    tilbakekrevingBehandling != null &&
    [
      TilbakekrevingStatus.VALIDERT,
      TilbakekrevingStatus.UNDERKJENT,
      TilbakekrevingStatus.ATTESTERT,
      TilbakekrevingStatus.FATTET_VEDTAK,
    ].includes(tilbakekrevingBehandling.status)
  )
}
