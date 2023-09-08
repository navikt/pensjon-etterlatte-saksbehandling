import { BodyShort, Button, Heading } from '@navikt/ds-react'
import React from 'react'
import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Innhold } from '~components/klage/styled'
import { useNavigate } from 'react-router-dom'
import { useKlage } from '~components/klage/useKlage'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'

export function KlageOppsummering() {
  const navigate = useNavigate()
  const klage = useKlage()
  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Oppsummering av klagebehandlingen
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      {/* Se EY-2592 */}
      <Innhold>
        <BodyShort>Hvis formkravene ikke er oppfylt, klage avslått-brevet som skal til bruker</BodyShort>

        <BodyShort>
          Hvis omgjøring / delvis omgjøring, vis tekst om at en revurdering opprettes når klagen ferdigstilles
        </BodyShort>

        <BodyShort>Hvis stadfestelse / delvis omgjøring, vis brevet til KA / bruker med innstilling</BodyShort>
      </Innhold>

      <KnapperWrapper>
        <Button variant="secondary" onClick={() => navigate(`/klage/${klage?.id}/vurdering`)}>
          Gå tilbake
        </Button>
        <Button variant="primary" onClick={() => alert('Vi støtter ikke behandling av klage enda :(')}>
          Send av gårde greier / lag revurdering / ferdigstill klagen
        </Button>
      </KnapperWrapper>
    </Content>
  )
}
