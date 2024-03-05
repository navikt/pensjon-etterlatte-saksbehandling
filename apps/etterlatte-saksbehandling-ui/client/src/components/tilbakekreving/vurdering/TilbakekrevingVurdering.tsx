import { Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingOverordnet } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingOverordnet'
import { TilbakekrevingVurderingPerioder } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingPerioder'
import { TilbakekrevingVurderingOppsummering } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingOppsummering'
import { TilbakekrevingVurderingPerioderVisning } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingPerioderVisning'
import { TilbakekrevingVurderingOverordnetVisning } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingOverordnetVisning'

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
      {redigerbar ? (
        <>
          <TilbakekrevingVurderingOverordnet behandling={behandling} redigerbar={redigerbar} />
          <TilbakekrevingVurderingPerioder behandling={behandling} redigerbar={redigerbar} />
        </>
      ) : (
        <>
          <TilbakekrevingVurderingOverordnetVisning behandling={behandling} />
          <TilbakekrevingVurderingPerioderVisning behandling={behandling} />
        </>
      )}
      <TilbakekrevingVurderingOppsummering behandling={behandling} />
      <Border />
      <FlexRow $spacing={true} justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/brev`)}>
          {redigerbar ? 'Opprett vedtak' : 'Gå til vedtak'}
        </Button>
      </FlexRow>
    </Content>
  )
}
