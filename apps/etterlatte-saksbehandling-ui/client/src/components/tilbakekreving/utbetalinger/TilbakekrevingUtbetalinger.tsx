import { Heading } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
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
          <TilbakekrevingVurderingPerioderSkjema behandling={behandling} redigerbar={redigerbar} />
        </>
      ) : (
        <>
          <TilbakekrevingVurderingPerioderVisning behandling={behandling} />
        </>
      )}
    </Content>
  )
}
