import React, { useEffect, useState } from 'react'
import { Alert, Button, Heading, HStack, Radio, RadioGroup, Tag, VStack } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { useNavigate } from 'react-router-dom'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterDato } from '~utils/formatering/dato'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { OppgaveHandling, settOppgaveHandling } from '~store/reducers/JournalfoeringOppgaveReducer'
import { FormWrapper } from '../BehandleJournalfoeringOppgave'
import { SidebarPanel } from '~shared/components/Sidebar'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonNavnOgFoedsel } from '~shared/api/pdltjenester'
import { isFailure, isSuccess } from '~shared/api/apiUtils'
import { formaterOppgaveStatus, formaterSakstype } from '~utils/formatering/formatering'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { logger } from '~utils/logger'
import { StatusPaaOppgaveFrist } from '~components/oppgavebenk/frist/StatusPaaOppgaveFrist'
import { SakType } from '~shared/types/sak'
import { IBehandlingStatus, Opprinnelse } from '~shared/types/IDetaljertBehandling'

export default function StartOppgavebehandling() {
  const { oppgave, journalpost, oppgaveHandling, sakMedBehandlinger } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const antallBehandlinger = sakMedBehandlinger?.behandlinger.length || 0
  const [tilhoererBruker, settTilhoererBruker] = useState(false)

  const [personResult, hentPerson] = useApiCall(hentPersonNavnOgFoedsel)

  const neste = () => {
    switch (oppgaveHandling) {
      case OppgaveHandling.NY_BEHANDLING:
        return navigate('nybehandling', { relative: 'path' })
      case OppgaveHandling.FERDIGSTILL_OPPGAVE:
        return navigate('ferdigstill', { relative: 'path' })
      case OppgaveHandling.NY_KLAGE:
        return navigate('oppretteklage', { relative: 'path' })
      case OppgaveHandling.SVAR_ETTEROPPGJOER:
        return navigate(`/svar-paa-etteroppgjoer/${oppgave?.id}`, {
          relative: 'path',
          state: { opprinnelse: Opprinnelse.JOURNALFOERING },
        })
    }
  }

  const harIverksattBehandlingPaaSak = () => {
    if (!!sakMedBehandlinger?.behandlinger?.length) {
      return (
        sakMedBehandlinger?.behandlinger.filter((behandling) => behandling.status === IBehandlingStatus.IVERKSATT)
          .length > 0
      )
    } else {
      return false
    }
  }

  useEffect(() => {
    if (journalpost?.bruker?.id) {
      hentPerson(journalpost?.bruker?.id, ({ foedselsnummer }) => {
        settTilhoererBruker(oppgave?.fnr === foedselsnummer)
      })
    } else {
      logger.generalWarning({
        msg: `Journalpostid ${journalpost?.journalpostId} mangler bruker id. sak: ${journalpost?.sak?.fagsakId}`,
      })
    }
  }, [])

  if (!journalpost?.bruker?.id) {
    return <Alert variant="error">Journalposten mangler bruker</Alert>
  }

  if (!oppgave) return null
  else if (!erOppgaveRedigerbar(oppgave.status))
    return <Alert variant="success">Oppgaven er allerede ferdigbehandlet!</Alert>

  return (
    <FormWrapper $column>
      <Heading size="medium" spacing>
        Behandle journalføringsoppgave
      </Heading>

      {journalpost ? (
        <>
          {antallBehandlinger > 0 ? (
            <Alert variant="info">Bruker har allerede {antallBehandlinger} behandling(er) i Gjenny</Alert>
          ) : (
            <Alert variant="info">Bruker har ingen behandlinger i Gjenny</Alert>
          )}

          {!temaTilhoererGjenny(journalpost) && (
            <Alert variant="warning">Journalposten tilhører tema {journalpost.tema}</Alert>
          )}
        </>
      ) : (
        <Alert variant="warning">Kan ikke behandle oppgaven uten journalpost</Alert>
      )}

      {isSuccess(personResult) && !tilhoererBruker && (
        <Alert variant="warning">
          Journalposten og oppgaven tilhører nå to ulike brukere. Hvis du skal opprette ny behandling eller klage på
          bruker tilknyttet journalposten, må du opprette ny journalføringsoppgave. Dette kan du gjøre fra
          dokumentoversikten til brukeren.
        </Alert>
      )}

      {isFailure(personResult) && <Alert variant="error">Kunne ikke hente person: {personResult.error.detail}</Alert>}

      <RadioGroup
        legend="Velg handling"
        size="small"
        onChange={(value) => {
          dispatch(settOppgaveHandling(value as OppgaveHandling))
        }}
        value={oppgaveHandling || ''}
      >
        <Radio
          value={OppgaveHandling.NY_BEHANDLING}
          description={
            harIverksattBehandlingPaaSak()
              ? 'Oppretter en ny revurdering i saken'
              : 'Opprett en ny førstegangsbehandling i saken'
          }
          disabled={!journalpost || !tilhoererBruker}
        >
          {harIverksattBehandlingPaaSak() ? 'Opprett revurdering' : 'Opprett førstegangsbehandling'}
        </Radio>
        <Radio
          value={OppgaveHandling.NY_KLAGE}
          description="Opprett ny klagebehandling"
          disabled={!journalpost || !tilhoererBruker}
        >
          Opprett klagebehandling
        </Radio>
        <Radio
          value={OppgaveHandling.FERDIGSTILL_OPPGAVE}
          description="Dersom oppgaven ikke er aktuell/relevant kan du ferdigstille den"
        >
          Ferdigstill oppgaven
        </Radio>
        {sakMedBehandlinger?.sak.sakType === SakType.OMSTILLINGSSTOENAD && (
          <Radio value={OppgaveHandling.SVAR_ETTEROPPGJOER} description="Hvis bruker har svart på etteroppgjøret">
            Mottatt svar etteroppgjør
          </Radio>
        )}
      </RadioGroup>
      <VStack gap="2">
        <HStack justify="center">
          <Button variant="primary" onClick={neste} disabled={!oppgaveHandling}>
            Neste
          </Button>
        </HStack>
        <HStack justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </HStack>
      </VStack>
    </FormWrapper>
  )
}

export const OppgaveDetaljer = ({ oppgave }: { oppgave: OppgaveDTO }) => (
  <SidebarPanel $border>
    <Heading size="small" spacing>
      Oppgavedetaljer
    </Heading>

    <VStack gap="4">
      <Info
        label="Type"
        tekst={
          <Tag variant="success" size="small">
            {formaterSakstype(oppgave.sakType)}
          </Tag>
        }
      />
      <Info
        label="Status"
        tekst={
          <Tag size="small" variant="alt1">
            {formaterOppgaveStatus(oppgave.status)}
          </Tag>
        }
      />
      <Info label="Saksbehandler" tekst={oppgave.saksbehandler?.navn || <i>Ikke tildelt</i>} />
      <Info label="Bruker" tekst={oppgave.fnr ? <PersonLink fnr={oppgave.fnr}>{oppgave.fnr}</PersonLink> : '-'} />
      <Info label="Opprettet" tekst={formaterDato(oppgave.opprettet)} />
      <Info
        label="Frist"
        tekst={<StatusPaaOppgaveFrist oppgaveFrist={oppgave.frist} oppgaveStatus={oppgave.status} />}
      />
    </VStack>
  </SidebarPanel>
)
