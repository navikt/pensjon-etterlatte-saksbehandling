import { Alert, BodyLong, BodyShort, Box, Button, Detail, Heading, List, ReadMore, VStack } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { ConfigContext } from '~clientConfig'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysninger, usePersonopplysningerOmsAvdoede } from '~components/person/usePersonopplysninger'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { formaterDato, formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { AktivitetspliktVurdering } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurdering'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { AktivitetspliktOppfolging } from '~shared/types/Aktivitetsplikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitetspliktOppfolging } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { isPending } from '~shared/api/apiUtils'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { isValid, parse } from 'date-fns'

const isValidDateOfDeath = (date?: Date) => {
  if (date) {
    const parsedDate = parse(String(date), 'yyyy-MM-dd', new Date())
    return isValid(parsedDate)
  }
  return false
}

export const Aktivitetsplikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()

  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const avdoede = usePersonopplysningerOmsAvdoede()
  const avdoedesDoedsdato = avdoede?.opplysning?.doedsdato
  const [aktivitetOppfolging, setAktivitetOppfolging] = useState<AktivitetspliktOppfolging>()
  const [manglerAktivitetspliktVurdering, setManglerAktivitetspliktVurdering] = useState<boolean | undefined>(undefined)

  const [hentet, hent] = useApiCall(hentAktivitetspliktOppfolging)

  const configContext = useContext(ConfigContext)
  const visTidslinje = useFeatureEnabledMedDefault('aktivitetsplikt-tidslinje', false)

  useEffect(() => {
    hent({ behandlingId: behandling.id }, (aktivitetOppfolging) => {
      setAktivitetOppfolging(aktivitetOppfolging)
    })
  }, [])

  const erFerdigUtfylt = () => {
    if (manglerAktivitetspliktVurdering === undefined || manglerAktivitetspliktVurdering) {
      setManglerAktivitetspliktVurdering(true)
      return
    }

    next()
  }

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
          <strong>Dødsdato: </strong> {avdoedesDoedsdato && formaterDato(avdoedesDoedsdato)}
        </BodyShort>
      </Box>

      <AktivitetspliktWrapper gap="10">
        <TekstWrapper>
          <Heading level="1" spacing size="medium">
            Gjenlevende sin situasjon
          </Heading>
          <BodyLong spacing>
            Det stilles ulike krav til aktivitet utifra tid etter dødsfallet. Seks måneder etter dødsfallet må
            gjenlevende være i minst 50 % aktivitet for å ha rett til omstillingsstønad. Videre kan det stilles krav til
            100 % aktivitet etter 12 måneder. I visse tilfeller kan man ha rett på omstillingsstønad selv om
            aktivitetskravet ikke er oppfylt.
          </BodyLong>
        </TekstWrapper>

        {visTidslinje && isValidDateOfDeath(avdoedesDoedsdato!!) && (
          <AktivitetspliktTidslinje behandling={behandling} doedsdato={avdoedesDoedsdato!!} />
        )}
        {visTidslinje && (
          <AktivitetspliktVurdering
            behandling={behandling}
            resetManglerAktivitetspliktVurdering={() => setManglerAktivitetspliktVurdering(false)}
          />
        )}

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

        <TekstWrapper>
          <Heading size="small">Oppfølging</Heading>
          <Box paddingBlock="4">
            <ReadMore header="Mer om oppfølging">
              <BodyLong spacing>
                Etterlatte skal følges opp og minnes på aktivitetskravet med informasjonsbrev rundt 4 måneder og igjen
                rundt 10 måneder etter dødsfallet. Interne oppfølgingsoppgaver opprettes automatisk ut fra
                dødstidspunktet, og brukers situasjon må vurderes før informasjonsbrevet sendes ut. Automatiske oppgaver
                blir opprettet som følge av hva du registrerer om brukers situasjon.
              </BodyLong>
              <BodyLong spacing>
                Det skal sendes et eget informasjonsbrev til de som ikke har aktivitetsplikt. Det opprettes en
                automatisk oppgave for å sende ut disse brevene rundt 6 måneder etter dødsfallet.
              </BodyLong>
              <BodyLong spacing>
                Er det andre grunner til at den etterlatte skal følges opp utenfor normalen, blant annet om bruker er
                midlertidig unntatt fra aktivitetsplikten, så må du opprette oppfølgingsoppgave i Gosys.
              </BodyLong>
            </ReadMore>
          </Box>
          <Button
            variant="secondary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppfølgingsoppgave i Gosys <ExternalLinkIcon />
          </Button>
        </TekstWrapper>

        {behandling.behandlingType === IBehandlingsType.REVURDERING && (
          <TekstWrapper>
            <Heading size="small">Status på informasjonsbrev</Heading>
            <BodyLong>
              Se hvilken dato infobrevet er sendt for å vurdere når du skal sende oppgave til lokalkontor (tre uker
              etter infobrevet er sendt ut), og når du eventuelt skal sende varsel om stans (tre uker før vedtak),
            </BodyLong>
          </TekstWrapper>
        )}

        <TekstWrapper>
          <Heading size="small" spacing>
            Trengs det oppfølging fra lokalkontor?
          </Heading>
          <BodyLong spacing>
            Trenger etterlatte ekstra oppfølging skal man sende oppgave til lokalkontor. Dette gjelder de som er utenfor
            arbeidslivet og/ eller ikke har varige ytelser fra Nav. Det er lokalkontor som skal følge opp disse
            brukerne, og de må derfor vite om at bruker har omstillingsstønad.
          </BodyLong>
          {behandling.behandlingType === IBehandlingsType.REVURDERING && (
            <BodyLong spacing>
              Kopier inn i vurderingen over det alternativet som gjelder:
              <List>
                <List.Item>Ja, har sendt oppgave om at bruker har omstillingsstønad og trenger oppfølging</List.Item>
                <List.Item>
                  Ja, har sendt oppgave om at bruker har omstillingsstønad, vi ser at hen er under oppfølging, og at de
                  må informere oss hvis brukers situasjon endrer seg
                </List.Item>
                <List.Item>Nei, unødvendig å sende oppgave</List.Item>
              </List>
            </BodyLong>
          )}
          <Button
            variant="secondary"
            size="small"
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppgave til lokalkontor <ExternalLinkIcon />
          </Button>
        </TekstWrapper>

        {manglerAktivitetspliktVurdering && (
          <Alert style={{ maxWidth: '16em' }} variant="error">
            Du må fylle ut vurdering om aktivitetsplikt
          </Alert>
        )}
      </AktivitetspliktWrapper>

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        <BehandlingHandlingKnapper>
          <Button variant="primary" onClick={() => erFerdigUtfylt()}>
            {handlinger.NESTE.navn}
          </Button>
        </BehandlingHandlingKnapper>
      </Box>
    </>
  )
}

const AktivitetspliktWrapper = styled(VStack)`
  padding: 0 var(--a-spacing-16) var(--a-spacing-8) var(--a-spacing-16);
  max-width: var(--a-breakpoint-2xl);
`

const TekstWrapper = styled.div`
  max-width: var(--a-breakpoint-md-down);
`
