import React from 'react'
import { Button, Heading, Panel, Radio, RadioGroup, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { useNavigate } from 'react-router-dom'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterFnr, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'

import { FristWrapper } from '~components/nyoppgavebenk/FristWrapper'
import { JournalpostVariant } from '~shared/types/Journalpost'
import { settJournalpostVariant } from '~store/reducers/JournalfoeringOppgaveReducer'
import { FormWrapper } from '../BehandleJournalfoeringOppgave'

export default function StartOppgavebehandling() {
  const { oppgave, journalpostVariant } = useJournalfoeringOppgave()
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
          Journalføringsoppgave
          <br />
          <Tag variant="success" size="small">
            {formaterSakstype(oppgave.sakType)}
          </Tag>
        </Heading>

        <InfoWrapper>
          <Info label="Status" tekst={oppgave.status} />
          <Info label="Type" tekst={oppgave.type} />
          <Info label="Bruker" tekst={formaterFnr(oppgave.fnr)} />
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
      >
        <Radio value={JournalpostVariant.NY_SOEKNAD}>Ny søknad</Radio>
        <Radio value={JournalpostVariant.NYTT_VEDLEGG}>Nytt vedlegg til sak</Radio>
        <Radio value={JournalpostVariant.FEIL_TEMA}>Feil tema</Radio>
      </RadioGroup>

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="primary" onClick={neste} disabled={!journalpostVariant}>
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
