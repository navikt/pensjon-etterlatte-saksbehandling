import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import '@navikt/ds-css'
import { Behandling } from '~components/behandling/Behandling'
import { HeaderBanner } from '~shared/header/HeaderBanner'
import { Person } from '~components/person/Person'
import useInnloggetSaksbehandler from './shared/hooks/useInnloggetSaksbehandler'
import nb from 'date-fns/locale/nb'
import { registerLocale } from 'react-datepicker'
import ErrorBoundary, { ApiErrorAlert } from '~ErrorBoundary'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import NyttBrev from '~components/person/brev/NyttBrev'
import ScrollToTop from '~ScrollTop'
import { ToggleNyOppgaveliste } from '~components/nyoppgavebenk/ToggleNyOppgavelist'
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { useEffect, useState } from 'react'
import { ConfigContext, hentClientConfig } from '~clientConfig'

function App() {
  const innloggetbrukerHentet = useInnloggetSaksbehandler()
  registerLocale('nb', nb)

  const [fetchConfigResultStatus, hentConfig] = useApiCall(hentClientConfig)
  const [config, setConfig] = useState({})

  useEffect(() => {
    hentConfig({}, (val) => setConfig(val))
  }, [])

  return (
    <>
      {isSuccess(fetchConfigResultStatus) && innloggetbrukerHentet && (
        <div className="app">
          <BrowserRouter basename="/">
            <ScrollToTop />
            <HeaderBanner />
            <ErrorBoundary>
              <Routes>
                <Route
                  path="/"
                  element={
                    <ConfigContext.Provider value={config}>
                      <ToggleNyOppgaveliste />
                    </ConfigContext.Provider>
                  }
                />
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

      {isFailure(fetchConfigResultStatus) && <ApiErrorAlert>Kunne ikke hente konfigurasjonsverdier</ApiErrorAlert>}
    </>
  )
}

export default App
