import { Box, Heading } from '@navikt/ds-react'
import { Content } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
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
    <Content>
      <Box paddingInline="16" paddingBlock="4">
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurdering
          </Heading>
        </HeadingWrapper>
      </Box>
      <TilbakekrevingVurderingSkjema behandling={behandling} redigerbar={redigerbar} />
    </Content>
  )
}
