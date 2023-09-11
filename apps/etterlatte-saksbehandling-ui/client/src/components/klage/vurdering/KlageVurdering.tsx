import { BodyShort, Button, Heading } from '@navikt/ds-react'
import React from 'react'
import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Innhold } from '~components/klage/styled'
import { useNavigate } from 'react-router-dom'
import { useKlage } from '~components/klage/useKlage'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'

export function KlageVurdering() {
  const navigate = useNavigate()
  const klage = useKlage()
  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Ta stilling til klagen
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      {/* Se EY-2591 */}
      <Innhold>
        <BodyShort>Velg om det er 1. omgjøring 2. delvis omgjøring 3. stadfestelse av vedtaket</BodyShort>

        <BodyShort>Hvis omgjøring / delvis omgjøring, velg hvorfor fra noen predefinerte grunner</BodyShort>

        <BodyShort>Hvis stadfestelse / delvis omgjøring, velg hovedparagraf i loven som klagen gjelder</BodyShort>
        <BodyShort>Hvis stadfestelse / delvis omgjøring, skriv inn innstillingstekst til KA / bruker</BodyShort>
      </Innhold>

      <KnapperWrapper>
        <Button variant="secondary" onClick={() => navigate(`/klage/${klage?.id}/formkrav`)}>
          Gå tilbake
        </Button>
        <Button variant="primary" onClick={() => navigate(`/klage/${klage?.id}/oppsummering`)}>
          Send inn vurdering av klagen
        </Button>
      </KnapperWrapper>
    </Content>
  )
}
