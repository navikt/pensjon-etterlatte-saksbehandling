import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import React from 'react'
import { KlageNavLenke } from '~components/klage/stegmeny/KlageNavLenke'

export function KlageStegmeny() {
  return (
    <StegMenyWrapper>
      <KlageNavLenke path="formkrav" description="Vurder formkrav" enabled={true} />
      <KlageNavLenke path="vurdering" description="Vurder klagen" enabled={true} />
      <KlageNavLenke path="oppsummering" description="Oppsummering" enabled={true} />
    </StegMenyWrapper>
  )
}
