import { Klage } from '~shared/types/Klage'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { BodyLong, Button, Heading } from '@navikt/ds-react'
import React from 'react'
import { forrigeSteg, nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { useNavigate } from 'react-router-dom'

export function KlageInfoInnhenting(props: { klage: Klage }) {
  const { klage } = props

  const navigate = useNavigate()
  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Hent inn informasjon fra søker
          </Heading>
        </HeadingWrapper>
        <BodyLong spacing={true}>
          Hent inn mer informasjon fra søker for å avklare om formkravene kan innfris.
          <br />
          Du kan lage nytt brev til søker fra brevsiden, og oppgaven kan inntil videre settes på vent ved å utvide
          fristen.
        </BodyLong>

        <FlexRow justify="left">
          <Button as="a" variant="tertiary" href={`/person/${klage.sak.ident}?fane=BREV`}>
            Brev til bruker
          </Button>
        </FlexRow>

        <FlexRow justify="center">
          <Button type="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'vurdering'))}>
            Gå tilbake
          </Button>
          <Button type="button" variant="primary" onClick={() => navigate(nesteSteg(klage, 'vurdering'))}>
            Neste
          </Button>
        </FlexRow>
      </ContentHeader>
    </Content>
  )
}
