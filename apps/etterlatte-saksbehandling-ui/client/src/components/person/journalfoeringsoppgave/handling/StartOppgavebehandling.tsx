import React from 'react'
import { Button, Heading, Link, Panel, Radio, RadioGroup, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { useNavigate } from 'react-router-dom'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterOppgaveStatus, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'

import { FristWrapper } from '~components/nyoppgavebenk/FristWrapper'
import { JournalpostVariant } from '~shared/types/Journalpost'
import { settJournalpostVariant } from '~store/reducers/JournalfoeringOppgaveReducer'
import { FormWrapper } from '../BehandleJournalfoeringOppgave'

export default function StartOppgavebehandling() {
  const { oppgave, journalpost, journalpostVariant } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  const neste = () => {
    switch (journalpostVariant) {
      case JournalpostVariant.NY_SOEKNAD:
        return navigate('nybehandling', { relative: 'path' })
      case JournalpostVariant.NYTT_VEDLEGG:
        return navigate('ferdigstill', { relative: 'path' })
      case JournalpostVariant.FEIL_TEMA:
        return navigate('endretema', { relative: 'path' })
    }
  }

  if (!oppgave) return null

  return (
    <FormWrapper column>
      <Heading size="large">Behandle journalpost</Heading>

      <Panel border>
        <Heading size="medium" spacing>
          Oppgave
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

      <RadioGroup
        legend="Hva gjelder journalposten?"
        size="small"
        onChange={(value) => {
          dispatch(settJournalpostVariant(value as JournalpostVariant))
        }}
        value={journalpostVariant || ''}
        disabled={!journalpost}
      >
        <Radio value={JournalpostVariant.NY_SOEKNAD}>Ny søknad (behandling)</Radio>
        <Radio value={JournalpostVariant.NYTT_VEDLEGG}>Nytt vedlegg (tileggsinformasjon)</Radio>
        <Radio value={JournalpostVariant.FEIL_TEMA}>Feil tema</Radio>
      </RadioGroup>

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="primary" onClick={neste} disabled={!journalpostVariant || !journalpost}>
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
