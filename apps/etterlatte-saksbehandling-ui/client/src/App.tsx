import React, { useContext } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import '@navikt/ds-css'
import './App.css'
import { AppContext, IAppContext } from './store/AppContext'
import Oppgavebenken from './components/oppgavebenken/Oppgavebenken'
import { StatusBar, StatusBarTheme } from './components/statusbar'
import { Behandling } from './components/behandling'
import { Link } from 'react-router-dom'
import { Header } from './shared/header'
import { Person } from './components/person'
import useInnloggetSaksbehandler from './shared/hooks/useInnloggetSaksbehandler'


function App() {
  const innloggetbrukerHentet = useInnloggetSaksbehandler()

  const ctx = useContext<IAppContext>(AppContext)
  console.log(ctx)

  return (
    <>
      {innloggetbrukerHentet && (
        <div className="app">
          <BrowserRouter>
            <Header />
            <Routes>
              <Route
                path="/"
                element={
                  <>
                    <Link to="/behandling/815-493-00/personopplysninger">Gå til behandling</Link>
                    <Oppgavebenken />
                  </>
                }
              />
              <Route path="/oppgavebenken" element={<Oppgavebenken />} />
              <Route path="/person/:fnr" element={<Person />} />
              <Route
                path="/behandling/:behandlingId/*"
                element={
                  <>
                    <StatusBar theme={StatusBarTheme.gray} />
                    <Behandling />
                  </>
                }
              />
            </Routes>
          </BrowserRouter>
        </div>
      )}
    </>
  )
}

export default App
