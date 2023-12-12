import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import { Route, Routes, useParams } from 'react-router-dom'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import VelgJournalpost from '~components/person/journalfoeringsoppgave/VelgJournalpost'
import { Column, Container, GridContainer } from '~shared/styled'
import styled from 'styled-components'
import { settBruker, settNyBehandlingRequest, settOppgave, settSak } from '~store/reducers/JournalfoeringOppgaveReducer'
import { hentOppgave } from '~shared/api/oppgaver'
import { useAppDispatch } from '~store/Store'
import Spinner from '~shared/Spinner'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { isPending, isPendingOrInitial, isSuccess } from '~shared/api/apiUtils'
import { OppdaterJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/OppdaterJournalpost'
import { InnholdJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/InnholdJournalpost'
import StartOppgavebehandling from '~components/person/journalfoeringsoppgave/handling/StartOppgavebehandling'
import OpprettNyBehandling from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import OppsummeringOppgavebehandling from '~components/person/journalfoeringsoppgave/nybehandling/OppsummeringOppgavebehandling'
import FerdigstillOppgave from '~components/person/journalfoeringsoppgave/ferdigstilloppgave/FerdigstillOppgave'
import { kanEndreJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/validering'

export default function BehandleJournalfoeringOppgave() {
  const { nyBehandlingRequest, journalpost, oppgave, sakMedBehandlinger } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()

  const [oppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)
  const [sakStatus, apiHentSak] = useApiCall(hentSakMedBehandlnger)

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
        apiHentSak(oppgave.fnr, (sak) => {
          dispatch(settSak(sak))
        })
      })
    }
  }, [oppgaveId])

  if (isPendingOrInitial(oppgaveStatus)) {
    return <Spinner visible label="Henter oppgavedetaljer..." />
  } else if (isPending(sakStatus)) {
    return <Spinner visible label="Henter sak..." />
  }

  return (
    <>
      <NavigerTilbakeMeny label="Tilbake til oppgavebenken" path="/" />

      <GridContainer>
        <Column style={{ width: '40%' }}>
          <Container>
            {!!journalpost && !!sakMedBehandlinger ? (
              kanEndreJournalpost(journalpost) ? (
                <OppdaterJournalpost initialJournalpost={journalpost} sak={sakMedBehandlinger.sak} />
              ) : (
                <Routes>
                  <Route
                    index
                    element={<StartOppgavebehandling antallBehandlinger={sakMedBehandlinger.behandlinger.length} />}
                  />

                  <Route path="nybehandling">
                    <Route index element={<OpprettNyBehandling />} />
                    <Route path="oppsummering" element={<OppsummeringOppgavebehandling />} />
                  </Route>

                  <Route path="avslutt" element={<FerdigstillOppgave />} />
                </Routes>
              )
            ) : (
              <Spinner visible label="" />
            )}
          </Container>
        </Column>

        <Column>
          <Container>
            {isSuccess(oppgaveStatus) && <VelgJournalpost journalpostId={oppgave?.referanse || null} />}
            {!!journalpost && !kanEndreJournalpost(journalpost) && <InnholdJournalpost journalpost={journalpost} />}
          </Container>
        </Column>
      </GridContainer>
    </>
  )
}

export const FormWrapper = styled.div<{ column?: boolean }>`
  display: flex;
  flex-direction: ${(props) => (!!props.column ? 'column' : 'row')};
  gap: 1rem;
`
