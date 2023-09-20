import { BodyLong, BodyShort, Button, Detail, Heading, Textarea } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import { Content, ContentHeader } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import styled from 'styled-components'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { ConfigContext } from '~clientConfig'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitetspliktOppfolging, opprettAktivitetspliktOppfolging } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AktivitetspliktOppfolging } from '~shared/types/Aktivitetsplikt'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'

export const Aktivitetsplikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()

  const avdoedesDoedsdato = behandling?.familieforhold?.avdoede?.opplysning?.doedsdato
  const ferdigBehandlet = erFerdigBehandlet(behandling.status)
  const configContext = useContext(ConfigContext)

  const [beskrivelse, setBeskrivelse] = useState<string>('')
  const [aktivitetOppfolging, setAktivitetOppfolging] = useState<AktivitetspliktOppfolging>()
  const [hentet, hent] = useApiCall(hentAktivitetspliktOppfolging)
  const [lagret, lagre] = useApiCall(opprettAktivitetspliktOppfolging)

  useEffect(() => {
    hent({ behandlingId: behandling.id }, (aktivitetOppfolging) => {
      setAktivitetOppfolging(aktivitetOppfolging)
      setBeskrivelse(aktivitetOppfolging?.aktivitet)
    })
  }, [])

  function edit() {
    setAktivitetOppfolging(undefined)
  }

  return (
    <Content>
      {isFailure(hentet) && <ApiErrorAlert>En feil oppsto ved henting av data</ApiErrorAlert>}
      {isFailure(lagret) && <ApiErrorAlert>En feil oppsto ved lagring av data</ApiErrorAlert>}

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
          Det stilles ulike krav til aktivitet utifra tid etter dødsfallet. Seks måneder etter dødsfallet må gjenlevende
          være i minst 50 % aktivitet for å ha rett til omstillingsstønad. Videre kan det stilles krav til 100 %
          aktivitet etter 12 måneder. I visse tilfeller kan man ha rett på omstillingsstønad selv om aktivitetskravet
          ikke er oppfylt.
        </BodyLong>

        <Heading size={'small'} spacing>
          Beskriv etterlatte sin aktivitet idag
        </Heading>

        <Spinner visible={isPending(hentet)} label="Henter data" />

        {!isPending(hentet) && aktivitetOppfolging && (
          <SpacingWrapper>
            <BodyLong>{aktivitetOppfolging.aktivitet}</BodyLong>
            {!ferdigBehandlet && (
              <Button variant="tertiary" icon={<PencilIcon />} onClick={() => edit()}>
                Rediger
              </Button>
            )}
            <Detail>Manuelt av {aktivitetOppfolging?.opprettetAv}</Detail>
            <Detail>Sist endret {aktivitetOppfolging?.opprettet}</Detail>
          </SpacingWrapper>
        )}

        {!isPending(hentet) && !aktivitetOppfolging && (
          <>
            <Textarea
              label="Beskriv etterlatte sin aktivitet idag"
              hideLabel={true}
              size="medium"
              value={beskrivelse}
              onChange={(e) => setBeskrivelse(e.target.value)}
            />
            <SpacingWrapper>
              <Button
                variant="primary"
                size="small"
                onClick={() =>
                  lagre({ behandlingId: behandling.id, request: { aktivitet: beskrivelse } }, (lagretElement) =>
                    setAktivitetOppfolging(lagretElement)
                  )
                }
              >
                Lagre beskrivelse
              </Button>
            </SpacingWrapper>
          </>
        )}

        <Heading size={'small'} spacing>
          Lag intern oppfølgingsoppgave med riktig frist
        </Heading>
        <BodyLong spacing>
          Etterlatte skal følges opp og minnes på aktivitetskravet når det har gått 3-4 måneder og på nytt når det har
          gått 9-10 måneder etter dødsfall. Om den etterlatte har andre ytelser eller annen grunn til videre oppfølging
          bør man vurdere andre frister.
        </BodyLong>
        <SpacingWrapper>
          <Button
            variant="primary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${behandling.søker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppfølgingsoppgave i Gosys <ExternalLinkIcon />
          </Button>
        </SpacingWrapper>

        <Heading size={'small'} spacing>
          Er oppfølging av lokalkontor nødvendig?
        </Heading>
        <BodyLong spacing>
          Trenger etterlatte ekstra oppfølging skal man sende oppgave til lokalkontor. Dette gjelder de som er utenfor
          arbeidslivet og/ eller ikke har andre ytelser fra Nav.
        </BodyLong>
        <SpacingWrapper>
          <Button
            variant="secondary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${behandling.søker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppgave til lokalkontor <ExternalLinkIcon />
          </Button>
        </SpacingWrapper>
      </AktivitetspliktWrapper>

      <Border />

      <BehandlingHandlingKnapper>
        <Button variant="primary" onClick={() => next()}>
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
const SpacingWrapper = styled.div`
  margin-top: 1rem;
  margin-bottom: 2rem;
`
