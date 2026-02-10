import { StegMenyBox } from '~components/behandling/stegmeny/StegMeny'
import React from 'react'
import { TilbakekrevingNavLenke } from '~components/tilbakekreving/stegmeny/TilbakekrevingNavLenke'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { TilbakekrevingBehandling, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { HStack } from '@navikt/ds-react'

export function TilbakekrevingStegmeny() {
  const tilbakekrevingBehandling = useTilbakekreving()

  return (
    <StegMenyBox>
      <HStack gap="space-6" align="center">
        <TilbakekrevingNavLenke path="vurdering" description="Vurdering" enabled separator />
        <TilbakekrevingNavLenke path="utbetalinger" description="Utbetalinger" enabled separator />
        <TilbakekrevingNavLenke
          path="oppsummering"
          description="Oppsummering"
          enabled
          separator={!!tilbakekrevingBehandling?.sendeBrev}
        />
        {tilbakekrevingBehandling?.sendeBrev && (
          <TilbakekrevingNavLenke
            path="brev"
            description="Brev"
            enabled={kanSeBrev(tilbakekrevingBehandling)}
            separator={false}
          />
        )}
      </HStack>
    </StegMenyBox>
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
