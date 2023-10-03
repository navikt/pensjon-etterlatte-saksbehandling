import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import { Tilbakekreving } from '~shared/types/Tilbakekreving'

export function TilbakerevingsOppsummering({ tilbakekreving }: { tilbakekreving: Tilbakekreving }) {
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
      <InnholdPadding>
        <BodyShort>Relevante ting for oppsummering her</BodyShort>
      </InnholdPadding>
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/brev`)}>
          Gå videre til brev
        </Button>
      </FlexRow>
    </Content>
  )
}
