import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'

export function TilbakekrevingVedtak() {
  const tilbakekreving = useTilbakekreving()
  const navigate = useNavigate()

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vedtak
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <BodyShort>Relevante ting for vedtak her</BodyShort>
      </InnholdPadding>
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/vedtak`)}>
          Fatt vedtak
        </Button>
      </FlexRow>
    </Content>
  )
}
