import { Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingOverordnet } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingOverordnet'
import { TilbakekrevingVurderingPerioder } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingPerioder'
import { TilbakekrevingVurderingOppsummering } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingOppsummering'

export function TilbakekrevingVurdering({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const navigate = useNavigate()
  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <TilbakekrevingVurderingOverordnet behandling={behandling} />
      <TilbakekrevingVurderingPerioder behandling={behandling} />
      <TilbakekrevingVurderingOppsummering behandling={behandling} />
      <FlexRow $spacing={true} justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/brev`)}>
          Opprett vedtak
        </Button>
      </FlexRow>
    </Content>
  )
}
