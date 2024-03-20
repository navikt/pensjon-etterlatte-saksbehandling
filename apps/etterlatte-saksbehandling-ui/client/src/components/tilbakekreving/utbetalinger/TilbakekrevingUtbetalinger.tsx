import { Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingPerioder } from '~components/tilbakekreving/utbetalinger/TilbakekrevingVurderingPerioder'
import { TilbakekrevingVurderingPerioderVisning } from '~components/tilbakekreving/utbetalinger/TilbakekrevingVurderingPerioderVisning'

export function TilbakekrevingUtbetalinger({
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
            Utbetalinger
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      {redigerbar ? (
        <>
          <TilbakekrevingVurderingPerioder behandling={behandling} redigerbar={redigerbar} />
        </>
      ) : (
        <>
          <TilbakekrevingVurderingPerioderVisning behandling={behandling} />
        </>
      )}
      <Border style={{ marginTop: '3em' }} />
      <FlexRow $spacing={true} justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/oppsummering`)}>
          Neste
        </Button>
      </FlexRow>
    </Content>
  )
}
