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
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { useEffect } from 'react'
import { ConfigContext, hentClientConfig } from '~clientConfig'
import BehandleJournalfoeringOppgave, {
  FEATURE_TOGGLE_KAN_BRUKE_OPPGAVEBEHANDLING,
} from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { settAppversion } from '~store/reducers/AppconfigReducer'
import Versioncheck from '~Versioncheck'
import { Klagebehandling } from '~components/klage/Klagebehandling'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/OpprettKlage'
import { ToggleMinOppgaveliste } from '~components/nyoppgavebenk/ToggleMinOppgaveliste'
import { Tilbakekrevingsbehandling } from '~components/tilbakekreving/Tilbakekrevingsbehandling'

function App() {
  const innloggetbrukerHentet = useInnloggetSaksbehandler()
  registerLocale('nb', nb)
  const dispatch = useAppDispatch()
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE)
  const kanBrukeOppgavebehandling = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_OPPGAVEBEHANDLING)
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
                  <Route path="/" element={<ToggleMinOppgaveliste />} />
                  <Route path="/oppgavebenken" element={<ToggleMinOppgaveliste />} />
                  <Route path="/person/:fnr" element={<Person />} />
                  {kanBrukeOppgavebehandling && (
                    <Route path="/oppgave/:id/*" element={<BehandleJournalfoeringOppgave />} />
                  )}
                  <Route path="/person/:fnr/sak/:sakId/brev" element={<BrevOversikt />} />
                  <Route path="/person/:fnr/sak/:sakId/brev/:brevId" element={<NyttBrev />} />
                  <Route path="/behandling/:behandlingId/*" element={<Behandling />} />
                  {kanBrukeKlage ? <Route path="/klage/:klageId/*" element={<Klagebehandling />} /> : null}
                  <Route path="/tilbakekreving/:tilbakekrevingId/*" element={<Tilbakekrevingsbehandling />} />
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </ConfigContext.Provider>
            </ErrorBoundary>
          </BrowserRouter>
        </div>
      )}

      {isFailure(hentConfigStatus) && <ApiErrorAlert>Kunne ikke hente konfigurasjonsverdier</ApiErrorAlert>}
    </>
  )
}

export default App
