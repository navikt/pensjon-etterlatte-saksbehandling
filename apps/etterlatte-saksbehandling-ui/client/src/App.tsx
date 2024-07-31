import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import '@navikt/ds-css'
import { Behandling } from '~components/behandling/Behandling'
import { HeaderBanner } from '~shared/header/HeaderBanner'
import { Person } from '~components/person/Person'
import useHentInnloggetSaksbehandler from 'src/shared/hooks/useSettInnloggetSaksbehandler'
import { nb } from 'date-fns/locale'
import { registerLocale } from 'react-datepicker'
import ErrorBoundary from '~ErrorBoundary'
import NyttBrev from '~components/person/brev/NyttBrev'
import ScrollToTop from '~ScrollTop'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import { ConfigContext, hentClientConfig } from '~clientConfig'
import { useAppDispatch } from '~store/Store'
import { settAppversion } from '~store/reducers/AppconfigReducer'
import Versioncheck from '~Versioncheck'
import { Klagebehandling } from '~components/klage/Klagebehandling'
import { Oppgavebenk } from '~components/oppgavebenk/Oppgavebenk'
import { Tilbakekrevingsbehandling } from '~components/tilbakekreving/Tilbakekrevingsbehandling'
import GenerellBehandling from '~components/generellbehandling/GenerellBehandling'
import ManuellBehandling from '~components/manuelbehandling/ManuellBehandling'
import BehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { setDefaultOptions } from 'date-fns'

function App() {
  const innloggetbrukerHentet = useHentInnloggetSaksbehandler()
  setDefaultOptions({ locale: nb })
  registerLocale('nb', nb)
  const dispatch = useAppDispatch()

  const [hentConfigStatus, hentConfig] = useApiCall(hentClientConfig)

  useEffect(() => {
    hentConfig({}, (res) => {
      dispatch(settAppversion(res.cachebuster))
    })
  }, [])

  return (
    <>
      {isSuccess(hentConfigStatus) && innloggetbrukerHentet && (
        <div className="app">
          <Versioncheck />
          <BrowserRouter basename="/">
            <ScrollToTop />
            <HeaderBanner />
            <ErrorBoundary>
              <ConfigContext.Provider value={hentConfigStatus.data}>
                <Routes>
                  <Route path="/" element={<Oppgavebenk />} />
                  <Route path="/oppgave/:id/*" element={<BehandleJournalfoeringOppgave />} />
                  <Route path="/person/:sakId" element={<Person />} />
                  <Route path="/person/:fnr/sak/:sakId/brev/:brevId" element={<NyttBrev />} />
                  <Route path="/behandling/:behandlingId/*" element={<Behandling />} />
                  <Route path="/manuellbehandling/*" element={<ManuellBehandling />} />
                  <Route path="/klage/:klageId/*" element={<Klagebehandling />} />
                  <Route path="/tilbakekreving/:tilbakekrevingId/*" element={<Tilbakekrevingsbehandling />} />
                  <Route path="/generellbehandling/:generellbehandlingId" element={<GenerellBehandling />} />
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </ConfigContext.Provider>
            </ErrorBoundary>
          </BrowserRouter>
        </div>
      )}
      {isFailureHandler({
        apiResult: hentConfigStatus,
        errorMessage: 'Kunne ikke hente konfigurasjonsverdier',
      })}
    </>
  )
}

export default App
