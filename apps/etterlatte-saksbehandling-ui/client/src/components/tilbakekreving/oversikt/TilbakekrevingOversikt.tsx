import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'

export function TilbakekrevingOversikt() {
  const tilbakekreving = useTilbakekreving()
  const navigate = useNavigate()

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Oversikt
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <Innhold>
        <BodyShort>Informasjon om tilbakekreving her</BodyShort>
      </Innhold>
      <KnapperWrapper>
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/vurdering`)}>
          Gå til vurdering
        </Button>
      </KnapperWrapper>
    </Content>
  )
}
