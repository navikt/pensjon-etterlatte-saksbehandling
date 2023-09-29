import { Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React from 'react'
import { Tilbakekreving } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingOverordnet } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingOverordnet'
import { TilbakekrevingVurderingPerioder } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingPerioder'

export function TilbakekrevingVurdering({ tilbakekreving }: { tilbakekreving: Tilbakekreving }) {
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
      <TilbakekrevingVurderingOverordnet tilbakekreving={tilbakekreving} />
      <TilbakekrevingVurderingPerioder tilbakekreving={tilbakekreving} />
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/vedtak`)}>
          Gå til vedtak
        </Button>
      </FlexRow>
    </Content>
  )
}
