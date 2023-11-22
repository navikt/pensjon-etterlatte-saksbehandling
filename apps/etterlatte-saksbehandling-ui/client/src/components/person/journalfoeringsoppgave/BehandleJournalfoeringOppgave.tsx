import { isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { useEffect } from 'react'
import { Route, Routes, useParams } from 'react-router-dom'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import VelgJournalpost from '~components/person/journalfoeringsoppgave/VelgJournalpost'
import { Column, Container, GridContainer } from '~shared/styled'
import styled from 'styled-components'
import { settBruker, settNyBehandlingRequest, settOppgave } from '~store/reducers/JournalfoeringOppgaveReducer'
import { hentOppgave } from '~shared/api/oppgaver'
import { useAppDispatch } from '~store/Store'
import StartOppgavebehandling from '~components/person/journalfoeringsoppgave/handling/StartOppgavebehandling'
import OpprettNyBehandling from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import OppsummeringOppgavebehandling from '~components/person/journalfoeringsoppgave/nybehandling/OppsummeringOppgavebehandling'
import Spinner from '~shared/Spinner'
import EndreJournalpostTema from '~components/person/journalfoeringsoppgave/endretema/EndreJournalpostTema'
import FerdigstillJournalpost from '~components/person/journalfoeringsoppgave/ferdigstilljournalpost/FerdigstillJournalpost'

export default function BehandleJournalfoeringOppgave() {
  const { nyBehandlingRequest, oppgave } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()

  const [oppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)

  const { id: oppgaveId } = useParams()

  useEffect(() => {
    if (!oppgave && oppgaveId) {
      apiHentOppgave(oppgaveId, (oppgave) => {
        dispatch(settOppgave(oppgave))
        dispatch(settBruker(oppgave.fnr))
        dispatch(
          settNyBehandlingRequest({
            ...nyBehandlingRequest,
            sakType: oppgave.sakType,
            persongalleri: { ...nyBehandlingRequest?.persongalleri, soeker: oppgave.fnr },
          })
        )
      })
    }
  }, [oppgaveId])

  if (isPendingOrInitial(oppgaveStatus)) {
    return <Spinner visible label="Henter oppgavedetaljer..." />
  }

  return (
    <>
      <NavigerTilbakeMeny label="Tilbake til oppgavebenken" path="/" />

      <GridContainer>
        <Column style={{ width: '50rem' }}>
          <Container>
            <Routes>
              <Route index element={<StartOppgavebehandling />} />

              <Route path="nybehandling">
                <Route index element={<OpprettNyBehandling />} />
                <Route path="oppsummering" element={<OppsummeringOppgavebehandling />} />
              </Route>

              <Route path="ferdigstill" element={<FerdigstillJournalpost />} />

              <Route path="endretema" element={<EndreJournalpostTema />} />
            </Routes>
          </Container>
        </Column>

        <Column>
          <Container>
            {isSuccess(oppgaveStatus) && <VelgJournalpost journalpostId={oppgave?.referanse || null} />}
          </Container>
        </Column>
      </GridContainer>
    </>
  )
}

export const FormWrapper = styled.div<{ column?: boolean }>`
  display: flex;
  flex-direction: ${(props) => (!!props.column ? 'column' : 'row')};
  gap: 2rem;
`
