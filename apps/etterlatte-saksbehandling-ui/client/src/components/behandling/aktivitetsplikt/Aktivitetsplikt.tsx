/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { Alert, BodyLong, BodyShort, Box, Button, Detail, Heading, List, ReadMore, VStack } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { ConfigContext } from '~clientConfig'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysninger, usePersonopplysningerOmsAvdoede } from '~components/person/usePersonopplysninger'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'
import { formaterDato, formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { AktivitetspliktVurdering } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurdering'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { AktivitetspliktOppfolging } from '~shared/types/Aktivitetsplikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitetspliktOppfolging } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { isPending } from '~shared/api/apiUtils'
import { isBefore, isValid, parse, subMonths } from 'date-fns'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

const isValidDateOfDeath = (date?: Date) => {
  if (date) {
    const parsedDate = parse(String(date), 'yyyy-MM-dd', new Date())
    return isValid(parsedDate)
  }
  return false
}

export const Aktivitetsplikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
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
  const [visFeilmelding, setVisFeilmelding] = useState<boolean>(false)

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
      setVisFeilmelding(true)
      return
    }

    next()
  }

  useEffect(() => {
    if (!manglerAktivitetspliktVurdering) {
      setVisFeilmelding(false)
    }
  }, [manglerAktivitetspliktVurdering])

  return (
    <>
      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av data',
        apiResult: hentetAktivitetspliktOppfoelgingStatus,
      })}
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading spacing size="large" level="1">
          Oppfølging av aktivitet
        </Heading>
        <BodyShort spacing>
          <strong>Avdødeds dødsdato: </strong> {avdoedesDoedsdato ? formaterDato(avdoedesDoedsdato) : 'Fant ingen dato'}
        </BodyShort>
      </Box>
      <VStack gap="space-8" paddingInline="space-16" paddingBlock="space-8">
        <VStack maxWidth="42.5rem">
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
              riktig oppfølging og forberede brukeren på kravene som følger.
            </BodyLong>
          </ReadMore>
        </VStack>

        {isValidDateOfDeath(avdoedesDoedsdato!) && ( // https://jira.adeo.no/browse/EY-4939 todo denne valideringen burde skje et annet sted vel og si noe om den ikke er gyldig
          <>
            <AktivitetspliktTidslinje behandling={behandling} doedsdato={avdoedesDoedsdato!} sakId={behandling.sakId} />
          </>
        )}
        {isBefore(avdoedesDoedsdato!, subMonths(Date.now(), 4)) && (
          <Box maxWidth="42.5rem">
            <Alert variant="info">
              Det har gått mer enn 4 måneder siden dødsfallet, det har derfor ikke blitt opprettet oppgave om
              aktivitetsplikt ved 6 måneder. Vurder aktiviteten til bruker før du sender informasjonsbrev om aktivitet,
              setter oppgaven på vent eller avslutter oppgaven.
            </Alert>
          </Box>
        )}

        <AktivitetspliktVurdering
          behandling={behandling}
          setManglerAktivitetspliktVurdering={setManglerAktivitetspliktVurdering}
          doedsdato={avdoedesDoedsdato!}
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
          <VStack maxWidth="42.5rem">
            <Heading size="small">Status på informasjonsbrev</Heading>
            <BodyLong>
              Se hvilken dato infobrevet er sendt for å vurdere når du skal sende oppgave til lokalkontor (tre uker
              etter infobrevet er sendt ut), og når du eventuelt skal sende varsel om stans (tre uker før vedtak),
            </BodyLong>
          </VStack>
        )}

        <VStack maxWidth="42.5rem">
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
              <Box marginBlock="space-16" asChild>
                <List data-aksel-migrated-v8>
                  <List.Item>Ja, har sendt oppgave om at bruker har omstillingsstønad og trenger oppfølging</List.Item>
                  <List.Item>
                    Ja, har sendt oppgave om at bruker har omstillingsstønad, vi ser at hen er under oppfølging, og at
                    de må informere oss hvis brukers situasjon endrer seg
                  </List.Item>
                  <List.Item>Nei, unødvendig å sende oppgave</List.Item>
                </List>
              </Box>
            </BodyLong>
          )}
          <div>
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
          </div>
        </VStack>

        {visFeilmelding && (
          <Box maxWidth="fit-content">
            <Alert variant="error">Du må fylle ut vurdering om aktivitetsplikt</Alert>
          </Box>
        )}
      </VStack>
      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
        <BehandlingHandlingKnapper>
          <Button variant="primary" onClick={() => erFerdigUtfylt()}>
            {handlinger.NESTE.navn}
          </Button>
        </BehandlingHandlingKnapper>
      </Box>
    </>
  )
}
