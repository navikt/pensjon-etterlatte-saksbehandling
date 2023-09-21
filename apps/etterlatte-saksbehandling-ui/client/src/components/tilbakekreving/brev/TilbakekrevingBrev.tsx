import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'

export function TilbakekrevingBrev() {
  const tilbakekreving = useTilbakekreving()
  const navigate = useNavigate()

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Brev
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <Innhold>
        <BodyShort>Relevante ting for brev her</BodyShort>
      </Innhold>
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/brev`)}>
          Send brev
        </Button>
      </FlexRow>
    </Content>
  )
}
