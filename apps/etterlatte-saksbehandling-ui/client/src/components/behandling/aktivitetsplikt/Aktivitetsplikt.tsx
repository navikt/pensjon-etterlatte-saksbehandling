import { BodyLong, BodyShort, Box, Button, Detail, Heading, VStack } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { ConfigContext } from '~clientConfig'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysninger, usePersonopplysningerOmsAvdoede } from '~components/person/usePersonopplysninger'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { formaterDato, formaterDatoMedKlokkeslett } from '~utils/formattering'
import { AktivitetspliktVurdering } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurdering'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { AktivitetspliktOppfolging } from '~shared/types/Aktivitetsplikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitetspliktOppfolging } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { isPending } from '~shared/api/apiUtils'

export const Aktivitetsplikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()

  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const avdoede = usePersonopplysningerOmsAvdoede()
  const avdoedesDoedsdato = avdoede?.opplysning?.doedsdato
  const [aktivitetOppfolging, setAktivitetOppfolging] = useState<AktivitetspliktOppfolging>()

  const [hentet, hent] = useApiCall(hentAktivitetspliktOppfolging)

  const configContext = useContext(ConfigContext)
  const visTidslinje = useFeatureEnabledMedDefault('aktivitetsplikt-tidslinje', false)

  useEffect(() => {
    hent({ behandlingId: behandling.id }, (aktivitetOppfolging) => {
      setAktivitetOppfolging(aktivitetOppfolging)
    })
  }, [])

  return (
    <>
      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av data',
        apiResult: hentet,
      })}

      <Box paddingInline="16" paddingBlock="16 4">
        <Heading spacing size="large" level="1">
          Oppfølging av aktivitet
        </Heading>
        <BodyShort spacing>
          <strong>Dødsdato: </strong> {avdoedesDoedsdato && formaterDato(new Date(avdoedesDoedsdato))}
        </BodyShort>
      </Box>

      <AktivitetspliktWrapper gap="10">
        <div>
          <Heading level="1" spacing size="medium">
            Gjenlevende sin situasjon
          </Heading>
          <BodyLong spacing>
            Det stilles ulike krav til aktivitet utifra tid etter dødsfallet. Seks måneder etter dødsfallet må
            gjenlevende være i minst 50 % aktivitet for å ha rett til omstillingsstønad. Videre kan det stilles krav til
            100 % aktivitet etter 12 måneder. I visse tilfeller kan man ha rett på omstillingsstønad selv om
            aktivitetskravet ikke er oppfylt.
          </BodyLong>
        </div>

        {visTidslinje && <AktivitetspliktTidslinje behandling={behandling} doedsdato={new Date(avdoedesDoedsdato!!)} />}
        {visTidslinje && <AktivitetspliktVurdering behandling={behandling} />}

        {aktivitetOppfolging && (
          <div>
            <Heading size="small">Beskriv etterlatte sin aktivitet idag</Heading>
            <Detail textColor="subtle" spacing>
              Dette er en vurdering som ble gjort før juni 2024
            </Detail>

            <Spinner visible={isPending(hentet)} label="Henter data" />

            {!isPending(hentet) && (
              <>
                <BodyLong>{aktivitetOppfolging.aktivitet}</BodyLong>

                <Detail>Manuelt av {aktivitetOppfolging?.opprettetAv}</Detail>
                <Detail>Sist endret {formaterDatoMedKlokkeslett(aktivitetOppfolging?.opprettet)}</Detail>
              </>
            )}
          </div>
        )}

        <div>
          <Heading size="small" spacing>
            OPPFØLGING
          </Heading>
          <BodyLong spacing>
            Etterlatte skal følges opp og minnes på aktivitetskravet med informasjonsbrev når det er gått 3 til 4
            måneder etter dødsfallet. Interne oppfølgingsoppgaver opprettes automatisk ut fra dødstidspunktet og må
            vurderes før informasjonsbrevet sendes ut. Automatiske oppgaver blir opprettet som følge av hva du
            registrerer om burkers situasjon.
            <br />
            <br />
            Er det andre grunner til at den etterlatte skal følges opp utenfor normalen, så kan oppfølgingsoppgave lages
            her:
          </BodyLong>
          <Button
            variant="secondary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppfølgingsoppgave i Gosys <ExternalLinkIcon />
          </Button>
        </div>

        <div>
          <Heading size="small" spacing>
            Er oppfølging av lokalkontor nødvendig?
          </Heading>
          <BodyLong spacing>
            Trenger etterlatte ekstra oppfølging skal man sende oppgave til lokalkontor. Dette gjelder de som er utenfor
            arbeidslivet og/ eller ikke har andre ytelser fra Nav.
          </BodyLong>
          <Button
            variant="secondary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppgave til lokalkontor <ExternalLinkIcon />
          </Button>
        </div>
      </AktivitetspliktWrapper>

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        <BehandlingHandlingKnapper>
          <Button variant="primary" onClick={() => next()}>
            {handlinger.NESTE.navn}
          </Button>
        </BehandlingHandlingKnapper>
      </Box>
    </>
  )
}

const AktivitetspliktWrapper = styled(VStack)`
  padding: 0 4em 2em 4em;
`
