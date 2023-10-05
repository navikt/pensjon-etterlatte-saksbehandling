import { BodyShort, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/sendTilAttesteringModal'
import { Tilbakekreving } from '~shared/types/Tilbakekreving'
import { fattVedtak } from '~shared/api/tilbakekreving'

export function TilbakekrevingBrev({ tilbakekreving }: { tilbakekreving: Tilbakekreving }) {
  const kanAttesteres = true

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Brev
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <BodyShort>Relevante ting for brev her</BodyShort>
      </InnholdPadding>
      <FlexRow justify="center">
        {kanAttesteres && <SendTilAttesteringModal behandlingId={tilbakekreving.id} fattVedtakApi={fattVedtak} />}
      </FlexRow>
    </Content>
  )
}
