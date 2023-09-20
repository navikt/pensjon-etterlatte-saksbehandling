import { useApiCall } from '~shared/hooks/useApiCall'
import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { getPerson } from '~shared/api/grunnlag'
import { GYLDIG_FNR } from '~utils/fnr'
import { StatusBar } from '~shared/statusbar/Statusbar'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import VelgJournalpost from '~components/person/journalfoeringsoppgave/VelgJournalpost'
import KontrollerOppgave from '~components/person/journalfoeringsoppgave/kontroll/KontrollerOppgave'
import OpprettNyBehandling from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { Column, Container, GridContainer } from '~shared/styled'
import OppsummeringOppgavebehandling from '~components/person/journalfoeringsoppgave/oppsummering/OppsummeringOppgavebehandling'
import JournalfoeringOppgaveStegmeny from '~components/person/journalfoeringsoppgave/JournalfoeringOppgaveStegmeny'
import styled from 'styled-components'

export const FEATURE_TOGGLE_KAN_BRUKE_OPPGAVEBEHANDLING = 'pensjon-etterlatte.kan-bruke-oppgavebehandling'

export default function BehandleJournalfoeringOppgave() {
  const { bruker } = useJournalfoeringOppgave()

  const [personStatus, hentPerson] = useApiCall(getPerson)

  useEffect(() => {
    if (GYLDIG_FNR(bruker)) {
      hentPerson(bruker!!, undefined, (error) => {
        if (error.statusCode === 404) {
          // TODO: Avventer design på hvordan vi skal håndtere visning av brukere som ikke finnes i vårt system
        }
      })
    }
  }, [bruker])

  return (
    <>
      <StatusBar result={personStatus} />
      <NavigerTilbakeMeny label="Tilbake til oppgavebenken" path="/" />

      <JournalfoeringOppgaveStegmeny />
      <GridContainer>
        <Column style={{ width: '50rem' }}>
          <Container>
            <Routes>
              <Route index element={<Navigate to="kontroll" />} />
              <Route path="/kontroll" element={<KontrollerOppgave />} />
              <Route path="/nybehandling" element={<OpprettNyBehandling />} />
              <Route path="/oppsummering" element={<OppsummeringOppgavebehandling />} />
            </Routes>
          </Container>
        </Column>

        <Column>
          <Container>
            <VelgJournalpost />
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
