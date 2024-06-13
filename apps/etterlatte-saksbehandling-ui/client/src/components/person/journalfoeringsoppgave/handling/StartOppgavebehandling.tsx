import React, { useEffect, useState } from 'react'
import { Alert, Button, Heading, HStack, Link, Radio, RadioGroup, Tag, VStack } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { useNavigate } from 'react-router-dom'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterOppgaveStatus, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import { OppgaveHandling, settOppgaveHandling } from '~store/reducers/JournalfoeringOppgaveReducer'
import { FormWrapper } from '../BehandleJournalfoeringOppgave'
import { SidebarPanel } from '~shared/components/Sidebar'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'
import { OppgaveDTO, erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonNavnogFoedsel } from '~shared/api/pdltjenester'
import { isSuccess } from '~shared/api/apiUtils'

export default function StartOppgavebehandling() {
  const { oppgave, journalpost, oppgaveHandling, sakMedBehandlinger } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const antallBehandlinger = sakMedBehandlinger?.behandlinger.length || 0
  const [tilhoererBruker, settTilhoererBruker] = useState(false)

  const [personResult, hentPerson] = useApiCall(hentPersonNavnogFoedsel)

  const neste = () => {
    switch (oppgaveHandling) {
      case OppgaveHandling.NY_BEHANDLING:
        return navigate('nybehandling', { relative: 'path' })
      case OppgaveHandling.FERDIGSTILL_OPPGAVE:
        return navigate('ferdigstill', { relative: 'path' })
      case OppgaveHandling.NY_KLAGE:
        return navigate('oppretteklage', { relative: 'path' })
    }
  }

  useEffect(() => {
    if (journalpost?.bruker?.id) {
      hentPerson(journalpost?.bruker?.id, ({ foedselsnummer }) => {
        settTilhoererBruker(oppgave?.fnr === foedselsnummer)
      })
    } else {
      throw Error('Journalposten mangler bruker')
    }
  }, [])

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
        <Alert variant="error">Journalposten tilhører ikke bruker som oppgaven er tilknyttet</Alert>
      )}

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
          description="Oppretter en ny behandling"
          disabled={!journalpost || !tilhoererBruker}
        >
          Opprett behandling
        </Radio>
        <Radio value={OppgaveHandling.NY_KLAGE} description="Opprett ny klagebehandling" disabled={!journalpost}>
          Opprett klagebehandling
        </Radio>
        <Radio
          value={OppgaveHandling.FERDIGSTILL_OPPGAVE}
          description="Dersom oppgaven ikke er aktuell/relevant kan du ferdigstille den"
        >
          Ferdigstill oppgaven
        </Radio>
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
      <Info
        label="Bruker"
        tekst={
          <Link href={`/person/${oppgave.fnr}`} target="_blank">
            {oppgave.fnr}
          </Link>
        }
      />
      <Info label="Opprettet" tekst={formaterStringDato(oppgave.opprettet)} />
      <Info label="Frist" tekst={<FristWrapper dato={oppgave.frist} />} />
    </VStack>
  </SidebarPanel>
)
