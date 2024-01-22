import { Alert, BodyLong, Button, Heading } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { FlexRow } from '~shared/styled'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import FerdigstillOppgaveModal from '~components/person/journalfoeringsoppgave/ferdigstilloppgave/FerdigstillOppgaveModal'

export default function FerdigstillOppgave() {
  const { journalpost, oppgave } = useJournalfoeringOppgave()

  const navigate = useNavigate()

  const tilbake = () => navigate('../', { relative: 'path' })

  if (!oppgave || !journalpost) return null

  const journalpostErFerdigstilt = ['FERDIGSTILT', 'JOURNALFOERT'].includes(journalpost.journalstatus)
  const journalpostTilhoererAnnetTema = !['EYO', 'EYB'].includes(journalpost.tema)
  const kanFerdigstilleOppgaven = journalpostErFerdigstilt || journalpostTilhoererAnnetTema

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Ferdigstill oppgave
      </Heading>

      <BodyLong spacing>
        Dersom journalposten allerede er ferdigstilt og/eller ikke relevant, kan oppgaven ferdigstilles.
      </BodyLong>

      {!kanFerdigstilleOppgaven && (
        <Alert variant="warning">
          Journalposten har status {journalpost?.journalstatus}. Oppgaven kan bare ferdigstilles når journalposten er
          ferdig behandlet.
        </Alert>
      )}

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <FerdigstillOppgaveModal oppgave={oppgave} kanFerdigstilles={kanFerdigstilleOppgaven} />
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
