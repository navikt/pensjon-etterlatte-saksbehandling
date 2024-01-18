import React from 'react'
import { Alert, Button, Heading, Link, Panel, Radio, RadioGroup, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { useNavigate } from 'react-router-dom'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterOppgaveStatus, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'

import { FristWrapper } from '~components/nyoppgavebenk/FristWrapper'
import { OppgaveHandling, settOppgaveHandling } from '~store/reducers/JournalfoeringOppgaveReducer'
import { FormWrapper } from '../BehandleJournalfoeringOppgave'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/KlageListe'

export default function StartOppgavebehandling({ antallBehandlinger }: { antallBehandlinger: number }) {
  const { oppgave, journalpost, oppgaveHandling } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)

  const neste = () => {
    switch (oppgaveHandling) {
      case OppgaveHandling.NY_BEHANDLING:
        return navigate('nybehandling', { relative: 'path' })
      case OppgaveHandling.FERDIGSTILL_OPPGAVE:
        return navigate('ferdigstill', { relative: 'path' })
      case OppgaveHandling.NY_KLAGE:
        if (kanBrukeKlage) {
          return navigate('oppretteklage', { relative: 'path' })
        } else {
          return navigate('../', { relative: 'path' })
        }
    }
  }

  if (!oppgave) return null

  return (
    <FormWrapper column>
      <Heading size="medium">Behandle journalføringsoppgave</Heading>

      <Panel border>
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
      </Panel>

      <br />
      {antallBehandlinger > 0 ? (
        <Alert variant="info">Bruker har allerede {antallBehandlinger} behandling(er) i Gjenny</Alert>
      ) : (
        <Alert variant="info">Bruker har ingen behandlinger i Gjenny</Alert>
      )}
      <br />

      <RadioGroup
        legend="Velg handling"
        size="small"
        onChange={(value) => {
          dispatch(settOppgaveHandling(value as OppgaveHandling))
        }}
        value={oppgaveHandling || ''}
        disabled={!journalpost}
      >
        <Radio value={OppgaveHandling.NY_BEHANDLING} description="Oppretter en ny behandling">
          Opprett behandling
        </Radio>
        <Radio
          value={OppgaveHandling.FERDIGSTILL_OPPGAVE}
          description="Dersom oppgaven ikke er aktuell/relevant kan du ferdigstille den"
        >
          Ferdigstill oppgaven
        </Radio>
        {kanBrukeKlage && (
          <Radio value={OppgaveHandling.NY_KLAGE} description="Opprett ny klagebehandling">
            Opprett klagebehandling
          </Radio>
        )}
      </RadioGroup>

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="primary" onClick={neste} disabled={!oppgaveHandling || !journalpost}>
            Neste
          </Button>
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
