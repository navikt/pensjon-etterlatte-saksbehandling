import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { useKlage } from '~components/klage/useKlage'
import { useNavigate } from 'react-router-dom'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'

export function KlageFormkrav() {
  const klage = useKlage()

  const navigate = useNavigate()

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurder formkrav
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      {/* Se EY-2590 */}
      <Innhold>
        <BodyShort>Knytt mot vedtak (trenger henting av iverksatte vedtak på saken)</BodyShort>
        <BodyShort>Er klager part i saken</BodyShort>
        <BodyShort>Er klagen signert av bruker</BodyShort>
        <BodyShort>Gjelder klagen noe konkret i vedtaket</BodyShort>
        <BodyShort>Er klagen framsatt innenfor fristen</BodyShort>

        <BodyShort>
          Er formkravene i klagen overholdt (kanskje overflødig -- vi kan la denne flyte fra resten? uansett vise
          resultatet)
        </BodyShort>
      </Innhold>
      <KnapperWrapper>
        <Button variant="primary" onClick={() => navigate(`/klage/${klage?.id}/vurdering`)}>
          Send inn vurdering av formkrav
        </Button>
      </KnapperWrapper>
    </Content>
  )
}
