import { Button, Heading, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { FlexRow } from '~shared/styled'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { useEffect } from 'react'
import { isInitial, isSuccess, mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentSakForPerson } from '~shared/api/sak'
import FerdigstillJournalpostModal from '~components/person/journalfoeringsoppgave/ferdigstilljournalpost/FerdigstillJournalpostModal'
import { formaterSakstype } from '~utils/formattering'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export default function FerdigstillJournalpost() {
  const { journalpost, oppgave } = useJournalfoeringOppgave()

  const navigate = useNavigate()

  const tilbake = () => navigate('../', { relative: 'path' })

  const [sakStatus, apiHentSak] = useApiCall(hentSakForPerson)

  useEffect(() => {
    if (isInitial(sakStatus) && !!oppgave) {
      apiHentSak({ fnr: oppgave.fnr, type: oppgave.sakType })
    }
  })

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Ferdigstill journalpost
      </Heading>

      {mapApiResult(
        sakStatus,
        <Spinner label="Henter sak ..." visible />,
        () => (
          <ApiErrorAlert>Feil oppsto ved henting av sak</ApiErrorAlert>
        ),
        (sak) => (
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
        )
      )}

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>
          {isSuccess(sakStatus) && (
            <FerdigstillJournalpostModal oppgave={oppgave!!} journalpost={journalpost!!} sak={sakStatus.data} />
          )}
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
