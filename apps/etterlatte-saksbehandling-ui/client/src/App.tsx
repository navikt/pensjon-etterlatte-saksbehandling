import React, { useContext } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import '@navikt/ds-css'
import './App.css'
import { AppContext, IAppContext } from './store/AppContext'
import Oppgavebenken from './components/oppgavebenken/Oppgavebenken'
import { Behandling } from './components/behandling'
import { Header } from './shared/header'
import { Person } from './components/person'
import useInnloggetSaksbehandler from './shared/hooks/useInnloggetSaksbehandler'
import { Testside } from './components/testside'


function App() {
  const innloggetbrukerHentet = useInnloggetSaksbehandler()

  const ctx = useContext<IAppContext>(AppContext)
  console.log(ctx)

  return (
    <>
      {innloggetbrukerHentet && (
        <div className="app">
          <BrowserRouter basename='/'>
            <Header />
            <Routes>
              <Route
                path="/"
                element={<Oppgavebenken />}
              />
              <Route path="/oppgavebenken" element={<Oppgavebenken />} />
              <Route path="/person/:fnr" element={<Person />} />
              <Route
                path="/behandling/:behandlingId/*"
                element={
                  <>
                    <Behandling />
                  </>
                }
              />
              <Route path="/test" element={<Testside />} />
            </Routes>
          </BrowserRouter>
        </div>
      )}
    </>
  )
}

export default App
