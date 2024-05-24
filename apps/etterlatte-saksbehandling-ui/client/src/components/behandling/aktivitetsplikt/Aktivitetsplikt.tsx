import { BodyLong, BodyShort, Box, Button, Detail, Heading, Textarea } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import { Content } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import styled from 'styled-components'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { ConfigContext } from '~clientConfig'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitetspliktOppfolging, opprettAktivitetspliktOppfolging } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { AktivitetspliktOppfolging } from '~shared/types/Aktivitetsplikt'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysninger, usePersonopplysningerOmsAvdoede } from '~components/person/usePersonopplysninger'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { formaterDato, formaterDatoMedKlokkeslett } from '~utils/formattering'

export const Aktivitetsplikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()

  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const avdoede = usePersonopplysningerOmsAvdoede()
  const avdoedesDoedsdato = avdoede?.opplysning?.doedsdato
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const configContext = useContext(ConfigContext)
  const visTidslinje = useFeatureEnabledMedDefault('aktivitetsplikt-tidslinje', false)

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
      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av data',
        apiResult: hentet,
      })}
      {isFailureHandler({
        errorMessage: 'En feil oppsto ved lagring av data',
        apiResult: lagret,
      })}

      <Box paddingInline="16" paddingBlock="4">
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Oppfølging av aktivitet
          </Heading>
          <BodyShort spacing>
            <strong>Dødsdato: </strong> {avdoedesDoedsdato && formaterDato(new Date(avdoedesDoedsdato))}
          </BodyShort>
        </HeadingWrapper>
      </Box>

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

        {visTidslinje && <AktivitetspliktTidslinje behandling={behandling} doedsdato={new Date(avdoedesDoedsdato!!)} />}

        <Heading size="small" spacing>
          Beskriv etterlatte sin aktivitet idag
        </Heading>

        <Spinner visible={isPending(hentet)} label="Henter data" />

        {!isPending(hentet) && aktivitetOppfolging && (
          <SpacingWrapper>
            <BodyLong>{aktivitetOppfolging.aktivitet}</BodyLong>
            {redigerbar && (
              <Button variant="tertiary" icon={<PencilIcon />} onClick={() => edit()}>
                Rediger
              </Button>
            )}
            <Detail>Manuelt av {aktivitetOppfolging?.opprettetAv}</Detail>
            <Detail>Sist endret {formaterDatoMedKlokkeslett(aktivitetOppfolging?.opprettet)}</Detail>
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

        <Heading size="small" spacing>
          OPPFØLGING
        </Heading>
        <BodyLong spacing>
          Etterlatte skal følges opp og minnes på aktivitetskravet med informasjonsbrev når det er gått 3 til 4 måneder
          etter dødsfallet. Interne oppfølgingsoppgaver opprettes automatisk ut fra dødstidspunktet og må vurderes før
          informasjonsbrevet sendes ut. Automatiske oppgaver blir opprettet som følge av hva du registrerer om burkers
          situasjon.
          <br />
          <br />
          Er det andre grunner til at den etterlatte skal følges opp utenfor normalen, så kan oppfølgingsoppgave lages
          her:
        </BodyLong>
        <SpacingWrapper>
          <Button
            variant="secondary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppfølgingsoppgave i Gosys <ExternalLinkIcon />
          </Button>
        </SpacingWrapper>

        <Heading size="small" spacing>
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
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppgave til lokalkontor <ExternalLinkIcon />
          </Button>
        </SpacingWrapper>
      </AktivitetspliktWrapper>

      <Border />

      <BehandlingHandlingKnapper>
        <Button variant="primary" onClick={() => next()}>
          {handlinger.NESTE.navn}
        </Button>
      </BehandlingHandlingKnapper>
    </Content>
  )
}

const AktivitetspliktWrapper = styled.div`
  padding: 0 4em;
`
const SpacingWrapper = styled.div`
  margin-top: 1rem;
  margin-bottom: 2rem;
`
