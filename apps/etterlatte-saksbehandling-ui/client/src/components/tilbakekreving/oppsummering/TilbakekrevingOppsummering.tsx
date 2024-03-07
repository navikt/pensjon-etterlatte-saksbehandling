import { Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingOppsummering } from '~components/tilbakekreving/oppsummering/TilbakekrevingVurderingOppsummering'

export function TilbakekrevingOppsummering({
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
            Oppsummering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <TilbakekrevingVurderingOppsummering behandling={behandling} />
      <Border style={{ marginTop: '3em' }} />
      <FlexRow $spacing={true} justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/brev`)}>
          {redigerbar ? 'Opprett vedtak' : 'Gå til vedtak'}
        </Button>
      </FlexRow>
    </Content>
  )
}
