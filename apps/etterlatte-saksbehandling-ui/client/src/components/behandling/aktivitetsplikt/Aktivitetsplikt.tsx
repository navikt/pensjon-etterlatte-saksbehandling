import { Alert, BodyLong, BodyShort, Box, Button, Detail, Heading, List, ReadMore, VStack } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { ConfigContext } from '~clientConfig'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
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
import { subMonths, isBefore, isValid, parse } from 'date-fns'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

const isValidDateOfDeath = (date?: Date) => {
  if (date) {
    const parsedDate = parse(String(date), 'yyyy-MM-dd', new Date())
    return isValid(parsedDate)
  }
  return false
}

export const Aktivitetsplikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const { next } = useContext(BehandlingRouteContext)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const avdoede = usePersonopplysningerOmsAvdoede()
  const avdoedesDoedsdato = avdoede?.opplysning?.doedsdato
  const [aktivitetOppfolging, setAktivitetOppfolging] = useState<AktivitetspliktOppfolging>()
  const [manglerAktivitetspliktVurdering, setManglerAktivitetspliktVurdering] = useState<boolean | undefined>(undefined)

  const [hentetAktivitetspliktOppfoelgingStatus, hentAktivitetspliktOppfoelging] =
    useApiCall(hentAktivitetspliktOppfolging)

  const configContext = useContext(ConfigContext)

  useEffect(() => {
    hentAktivitetspliktOppfoelging({ behandlingId: behandling.id }, (aktivitetOppfolging) => {
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
        apiResult: hentetAktivitetspliktOppfoelgingStatus,
      })}

      <Box paddingInline="16" paddingBlock="16 4">
        <Heading spacing size="large" level="1">
          Oppfølging av aktivitet
        </Heading>
        <BodyShort spacing>
          <strong>Avdødeds dødsdato: </strong> {avdoedesDoedsdato ? formaterDato(avdoedesDoedsdato) : 'Fant ingen dato'}
        </BodyShort>
      </Box>

      <AktivitetspliktWrapper gap="10">
        <TekstWrapper>
          <Heading level="1" spacing size="medium">
            Gjenlevende sin situasjon
          </Heading>
          <ReadMore header="Slik vurderer du gjenlevendes situasjon">
            <BodyLong spacing>
              Det stilles ulike krav til aktivitet basert på tiden som har gått etter dødsfallet. Seks måneder etter
              dødsfallet må den gjenlevende være i minst 50 % aktivitet for å ha rett til omstillingsstønad. Videre kan
              det kreves 100 % aktivitet etter tolv måneder. I enkelte tilfeller kan man likevel ha rett til
              omstillingsstønad, selv om aktivitetskravet ikke er oppfylt.
            </BodyLong>
            <BodyLong spacing>
              Selv om de første seks månedene er fritatt, er det viktig å kartlegge situasjonen tidlig for å sikre
              riktig oppfølging og forberede brukeren på kravene som følger
            </BodyLong>
          </ReadMore>
        </TekstWrapper>

        {isValidDateOfDeath(avdoedesDoedsdato!!) && ( // https://jira.adeo.no/browse/EY-4939 todo denne valideringen burde skje et annet sted vel og si noe om den ikke er gyldig
          <>
            <Heading spacing level="1" size="medium">
              Gjenlevende sin tidslinje
            </Heading>
            <AktivitetspliktTidslinje behandling={behandling} doedsdato={avdoedesDoedsdato!!} />
          </>
        )}
        {isBefore(avdoedesDoedsdato!!, subMonths(Date.now(), 4)) && (
          <Box maxWidth="42.5rem">
            <Alert variant="info">
              Det har gått mer enn 4 måneder siden dødsfallet, det har derfor ikke blitt opprettet oppgave om
              aktivtetsplikt ved 6 måneder. Vurder om du må manuelt sende informasjonsbrev og følg opp svar fra bruker.
            </Alert>
          </Box>
        )}

        <AktivitetspliktVurdering
          behandling={behandling}
          resetManglerAktivitetspliktVurdering={() => setManglerAktivitetspliktVurdering(false)}
          doedsdato={avdoedesDoedsdato!!}
        />

        {aktivitetOppfolging && (
          <div>
            <Heading size="small">Beskriv etterlatte sin aktivitet idag</Heading>
            <Detail textColor="subtle" spacing>
              Dette er en vurdering som ble gjort før juni 2024
            </Detail>

            {isPending(hentetAktivitetspliktOppfoelgingStatus) ? (
              <Spinner label="Henter data" />
            ) : (
              <>
                <BodyLong>{aktivitetOppfolging.aktivitet}</BodyLong>

                <Detail>Manuelt av {aktivitetOppfolging?.opprettetAv}</Detail>
                <Detail>Sist endret {formaterDatoMedKlokkeslett(aktivitetOppfolging?.opprettet)}</Detail>
              </>
            )}
          </div>
        )}

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
            disabled={!redigerbar}
            as="a"
            href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker?.foedselsnummer}`}
            target="_blank"
          >
            Lag oppgave til lokalkontor <ExternalLinkIcon aria-hidden />
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
