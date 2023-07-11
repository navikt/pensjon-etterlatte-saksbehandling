import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import '@navikt/ds-css'
import '@navikt/ds-css-internal'
import { Behandling } from '~components/behandling/Behandling'
import { HeaderBanner } from '~shared/header/HeaderBanner'
import { Person } from '~components/person/Person'
import useInnloggetSaksbehandler from './shared/hooks/useInnloggetSaksbehandler'
import nb from 'date-fns/locale/nb'
import { registerLocale } from 'react-datepicker'
import ErrorBoundary from '~ErrorBoundary'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import NyttBrev from '~components/person/brev/NyttBrev'
import ScrollToTop from '~ScrollTop'
import { ToggleNyOppgaveliste } from '~components/nyoppgavebenk/ToggleNyOppgavelist'

function App() {
  const innloggetbrukerHentet = useInnloggetSaksbehandler()
  registerLocale('nb', nb)

  return (
    <>
      {innloggetbrukerHentet && (
        <div className="app">
          <BrowserRouter basename="/">
            <ScrollToTop />
            <HeaderBanner />
            <ErrorBoundary>
              <Routes>
                <Route path="/" element={<ToggleNyOppgaveliste />} />
                <Route path="/oppgavebenken" element={<ToggleNyOppgaveliste />} />
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
