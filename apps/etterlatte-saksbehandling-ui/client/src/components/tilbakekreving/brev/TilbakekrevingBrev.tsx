import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
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
      <KnapperWrapper>
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/brev`)}>
          Send brev
        </Button>
      </KnapperWrapper>
    </Content>
  )
}
