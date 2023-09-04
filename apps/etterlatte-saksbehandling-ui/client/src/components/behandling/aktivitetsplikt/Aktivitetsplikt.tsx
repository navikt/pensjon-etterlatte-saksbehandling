import { BodyLong, BodyShort, Button, Heading, Textarea } from '@navikt/ds-react'
import React, { useContext } from 'react'
import { Content, ContentHeader } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import styled from 'styled-components'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { ConfigContext } from '~clientConfig'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const Aktivitetsplikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const avdoedesDoedsdato = behandling?.familieforhold?.avdoede?.opplysning?.doedsdato
  const configContext = useContext(ConfigContext)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Oppfølging av aktivitet
          </Heading>
          <BodyShort spacing>
            <strong>Dødsdato: </strong> {avdoedesDoedsdato}
          </BodyShort>
        </HeadingWrapper>
      </ContentHeader>

      <AktivitetspliktWrapper>
        <Heading level="1" spacing size="medium">
          Gjenlevende sin situasjon
        </Heading>
        <BodyLong spacing>
          Ut ifra tid etter dødsfall stilles det ulike krav til aktivitet hos søker. Innen det har gått 6 måneder etter
          dødsfall skal søker være i minst 50 % aktivitet for å motta omstillingstønad. Videre kan det forventes 100%
          aktivitet etter 12 måneder.
        </BodyLong>

        <Textarea label="Beskriv etterlatte sin aktivitet idag" size="medium" />
        <ButtonWrapper>
          <Button variant="primary" size="small" className="button" onClick={() => true}>
            Lagre beskrivelse
          </Button>
        </ButtonWrapper>

        <Heading size={'small'} spacing>
          Lag intern oppfølgingsoppgave med riktig frist
        </Heading>
        <BodyLong spacing>
          Etterlatte skal følges opp og minnes på aktivitetskravet når det har gått 3-4 måneder og på nytt når det har
          gått 9-10 måneder etter dødsfall. Om den etterlatte har andre ytelser eller annen grunn til videre oppfølging
          bør man vurdere andre frister.
        </BodyLong>
        <ButtonWrapper>
          <Button
            variant="primary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${behandling.søker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppfølgingsoppgave i Gosys <ExternalLinkIcon />
          </Button>
        </ButtonWrapper>

        <Heading size={'small'} spacing>
          Er oppfølging av lokalkontor nødvendig?
        </Heading>
        <BodyLong spacing>
          Trenger etterlatte ekstra oppfølging skal man sende oppgave til lokalkontor. Dette gjelder de som er utenfor
          arbeidslivet og/ eller ikke har andre ytelser fra Nav.
        </BodyLong>
        <ButtonWrapper>
          <Button variant="secondary" size="small" className="button" onClick={() => true}>
            Lag oppgave til lokalkontor <ExternalLinkIcon />
          </Button>
        </ButtonWrapper>
      </AktivitetspliktWrapper>

      <Border />

      <BehandlingHandlingKnapper>
        <Button variant="primary" size="medium" onClick={() => true}>
          Gå videre
        </Button>
      </BehandlingHandlingKnapper>
    </Content>
  )
}

const AktivitetspliktWrapper = styled.div`
  padding: 0 4em;
  max-width: 40em;
`
const ButtonWrapper = styled.div`
  margin-top: 1rem;
  margin-bottom: 2rem;
`
