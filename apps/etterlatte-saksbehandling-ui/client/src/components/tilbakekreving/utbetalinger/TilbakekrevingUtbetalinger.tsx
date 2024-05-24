import { Box, Heading } from '@navikt/ds-react'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingPerioderSkjema } from '~components/tilbakekreving/utbetalinger/TilbakekrevingVurderingPerioderSkjema'
import { TilbakekrevingVurderingPerioderVisning } from '~components/tilbakekreving/utbetalinger/TilbakekrevingVurderingPerioderVisning'

export function TilbakekrevingUtbetalinger({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  return (
    <>
      <Box paddingInline="16" paddingBlock="4">
        <HeadingWrapper>
          <Heading level="1" size="large">
            Utbetalinger
          </Heading>
        </HeadingWrapper>
      </Box>
      {redigerbar ? (
        <>
          <TilbakekrevingVurderingPerioderSkjema behandling={behandling} redigerbar={redigerbar} />
        </>
      ) : (
        <>
          <TilbakekrevingVurderingPerioderVisning behandling={behandling} />
        </>
      )}
    </>
  )
}
