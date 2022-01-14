import React, { useContext } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import '@navikt/ds-css'
import './App.css'
import { AppContext, IAppContext } from './store/AppContext'
import { ws } from './mocks/wsmock'
import Oppgavebenken from './components/oppgavebenken/Oppgavebenken'
import { Container } from './shared/styled'
import { Modal } from './shared/modal/modal'
import { StatusBar, StatusBarTheme } from './components/statusbar'
import { Behandling } from './components/behandling'
import { Link } from 'react-router-dom'
import { Header } from './shared/header'
import { Person } from './components/person'

ws()

function App() {
  const ctx = useContext<IAppContext>(AppContext)
  console.log(ctx)
  return (
    <div className="app">
      <BrowserRouter>
        <Header />

        <Routes>
          <Route
            path="/"
            element={
              // dras ut i egen component
              <>
                <Link to="/behandling/personopplysninger">Gå til behandling</Link>
                <Oppgavebenken />
              </>
            }
          />
          <Route path="/oppgavebenken" element={<Oppgavebenken />} />
          <Route path="/person/:fnr" element={<Person />} />
          <Route
            path="/behandling/*"
            element={
              <>
                <StatusBar theme={StatusBarTheme.gray} />
                <Behandling />
              </>
            }
          />
          <Route
            path="/testside"
            element={
              <Container>
                <Modal
                  onClose={() => {
                    console.log('Lukker modal')
                  }}
                >
                  Test
                </Modal>
              </Container>
            }
          />
        </Routes>
      </BrowserRouter>
    </div>
  )
}

export default App
