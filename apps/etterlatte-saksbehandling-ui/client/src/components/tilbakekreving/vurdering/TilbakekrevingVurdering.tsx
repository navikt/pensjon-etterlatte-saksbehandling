import { Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
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
      <TilbakekrevingVurderingSkjema behandling={behandling} redigerbar={redigerbar} />
      <Border style={{ marginTop: '3em' }} />
      <FlexRow $spacing={true} justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/utbetalinger`)}>
          Neste
        </Button>
      </FlexRow>
    </Content>
  )
}
