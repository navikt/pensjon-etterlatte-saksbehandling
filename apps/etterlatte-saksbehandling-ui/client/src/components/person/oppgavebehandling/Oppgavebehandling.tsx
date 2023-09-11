import { useApiCall } from '~shared/hooks/useApiCall'
import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { getPerson } from '~shared/api/grunnlag'
import { GYLDIG_FNR } from '~utils/fnr'
import { StatusBar } from '~shared/statusbar/Statusbar'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { useNyBehandling } from '~components/person/oppgavebehandling/useNyBehandling'
import VelgJournalpost from '~components/person/oppgavebehandling/VelgJournalpost'
import KontrollerOppgave from '~components/person/oppgavebehandling/kontroll/KontrollerOppgave'
import OpprettNyBehandling from '~components/person/oppgavebehandling/nybehandling/OpprettNyBehandling'
import { Column, Container, GridContainer } from '~shared/styled'
import OppsummeringOppgavebehandling from '~components/person/oppgavebehandling/oppsummering/OppsummeringOppgavebehandling'
import OppgaveStegmeny from '~components/person/oppgavebehandling/OppgaveStegmeny'

export default function Oppgavebehandling() {
  const { bruker } = useNyBehandling()

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
      <NavigerTilbakeMeny label={'Tilbake til oppgavebenken'} path={'/'} />

      <OppgaveStegmeny />
      <GridContainer>
        <Column style={{ width: '50rem' }}>
          <Container>
            <Routes>
              <Route index element={<Navigate to={'kontroll'} />} />
              <Route path={'/kontroll'} element={<KontrollerOppgave />} />
              <Route path={'/nybehandling'} element={<OpprettNyBehandling />} />
              <Route path={'/oppsummering'} element={<OppsummeringOppgavebehandling />} />
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
