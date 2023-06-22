import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import '@navikt/ds-css'
import '@navikt/ds-css-internal'
import Oppgavebenken from './components/oppgavebenken/Oppgavebenken'
import { Behandling } from '~components/behandling/Behandling'
import { HeaderWrapper } from '~shared/header/Header'
import { Person } from '~components/person/Person'
import useInnloggetSaksbehandler from './shared/hooks/useInnloggetSaksbehandler'
import nb from 'date-fns/locale/nb'
import { registerLocale } from 'react-datepicker'
import ErrorBoundary from '~ErrorBoundary'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import NyttBrev from '~components/person/brev/NyttBrev'
import ScrollToTop from '~ScrollTop'

function App() {
  const innloggetbrukerHentet = useInnloggetSaksbehandler()
  registerLocale('nb', nb)

  return (
    <>
      {innloggetbrukerHentet && (
        <div className="app">
          <BrowserRouter basename="/">
            <ScrollToTop />
            <HeaderWrapper />
            <ErrorBoundary>
              <Routes>
                <Route path="/" element={<Oppgavebenken />} />
                <Route path="/oppgavebenken" element={<Oppgavebenken />} />
                <Route path="/person/:fnr" element={<Person />} />
                <Route path="/person/:fnr/sak/:sakId/brev" element={<BrevOversikt />} />
                <Route path="/person/:fnr/sak/:sakId/brev/:brevId" element={<NyttBrev />} />
                <Route path="/behandling/:behandlingId/*" element={<Behandling />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </ErrorBoundary>
          </BrowserRouter>
        </div>
      )}
    </>
  )
}

export default App
