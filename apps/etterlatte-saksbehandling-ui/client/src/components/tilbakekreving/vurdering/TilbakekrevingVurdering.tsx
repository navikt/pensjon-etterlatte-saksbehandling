import { Box, Heading } from '@navikt/ds-react'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingSkjema } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingSkjema'

export function TilbakekrevingVurdering({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading level="1" size="large">
          Vurdering
        </Heading>
      </Box>
      {behandling && <TilbakekrevingVurderingSkjema behandling={behandling} redigerbar={redigerbar} />}
    </>
  )
}
