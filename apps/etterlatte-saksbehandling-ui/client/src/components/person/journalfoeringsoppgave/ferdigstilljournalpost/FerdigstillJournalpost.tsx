import { BodyShort, Button, Heading, Panel, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { FlexRow } from '~shared/styled'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import FerdigstillJournalpostModal from '~components/person/journalfoeringsoppgave/ferdigstilljournalpost/FerdigstillJournalpostModal'
import { formaterSakstype } from '~utils/formattering'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'

export default function FerdigstillJournalpost() {
  const { journalpost, oppgave, sak } = useJournalfoeringOppgave()

  const navigate = useNavigate()

  const tilbake = () => navigate('../', { relative: 'path' })

  if (!sak || !journalpost || !oppgave) return null

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Koble informasjon til sak
      </Heading>

      <BodyShort spacing>Journalposten vil bli koblet til sak i Gjenny og ferdigstilt.</BodyShort>

      <Panel border>
        <Heading size="medium" spacing>
          Sak
        </Heading>

        <InfoWrapper>
          <Info label="ID" tekst={sak.id} />
          <Info
            label="Type"
            tekst={
              <Tag variant="success" size="medium">
                {formaterSakstype(sak.sakType)}
              </Tag>
            }
          />
          <Info label="Bruker" tekst={sak.ident} />
          <Info label="Enhet" tekst={sak.enhet} />
        </InfoWrapper>
      </Panel>

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>
          <FerdigstillJournalpostModal oppgave={oppgave} journalpost={journalpost} sak={sak} />
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
