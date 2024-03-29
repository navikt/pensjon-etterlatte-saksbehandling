import React from 'react'
import { Alert, Button, Heading, Link, Radio, RadioGroup, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { useNavigate } from 'react-router-dom'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterOppgaveStatus, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import { OppgaveHandling, settOppgaveHandling } from '~store/reducers/JournalfoeringOppgaveReducer'
import { FormWrapper } from '../BehandleJournalfoeringOppgave'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/api/oppgaver'
import { SidebarPanel } from '~shared/components/Sidebar'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'

export default function StartOppgavebehandling({ antallBehandlinger }: { antallBehandlinger: number }) {
  const { oppgave, journalpost, oppgaveHandling } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

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

  if (!oppgave) return null
  else if (!erOppgaveRedigerbar(oppgave.status))
    return <Alert variant="success">Oppgaven er allerede ferdigbehandlet!</Alert>

  return (
    <FormWrapper column>
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

      <RadioGroup
        legend="Velg handling"
        size="small"
        onChange={(value) => {
          dispatch(settOppgaveHandling(value as OppgaveHandling))
        }}
        value={oppgaveHandling || ''}
      >
        <Radio value={OppgaveHandling.NY_BEHANDLING} description="Oppretter en ny behandling" disabled={!journalpost}>
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

      <FlexRow justify="center" $spacing>
        <Button variant="primary" onClick={neste} disabled={!oppgaveHandling}>
          Neste
        </Button>
      </FlexRow>
      <FlexRow justify="center">
        <AvbrytBehandleJournalfoeringOppgave />
      </FlexRow>
    </FormWrapper>
  )
}

export const OppgaveDetaljer = ({ oppgave }: { oppgave: OppgaveDTO }) => (
  <SidebarPanel border>
    <Heading size="small" spacing>
      Oppgavedetaljer
    </Heading>

    <InfoWrapper>
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
    </InfoWrapper>
  </SidebarPanel>
)
