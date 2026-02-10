import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import '@navikt/ds-css/dist/index.css'
import { Behandling } from '~components/behandling/Behandling'
import { HeaderBanner } from '~shared/header/HeaderBanner'
import { Person } from '~components/person/Person'
import useHentInnloggetSaksbehandler from 'src/shared/hooks/useSettInnloggetSaksbehandler'
import { nb } from 'date-fns/locale'
import ErrorBoundary, { ApiErrorAlert } from '~ErrorBoundary'
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
import { mapResult } from '~shared/api/apiUtils'
import { setDefaultOptions } from 'date-fns'
import GenerellOppgave from '~components/generelloppgave/GenerellOppgave'
import { VurderAktivitetspliktOppgave } from '~components/aktivitetsplikt/VurderAktivitetspliktOppgave'
import { Unleashcontext, useUnleash } from '~useUnleash'
import { EtteroppgjoerForbehandling } from '~components/etteroppgjoer/forbehandling/EtteroppgjoerForbehandling'
import { MeldtInnEndring } from '~components/meldtInnEndring/MeldtInnEndring'
import { useLastUmami } from '~shared/umami/useLastUmami'
import { SvarPaaEtteroppgjoer } from '~components/person/journalfoeringsoppgave/svarPaaEtteroppgjoer/SvarPaaEtteroppgjoer'

function App() {
  const innloggetbrukerHentet = useHentInnloggetSaksbehandler()

  setDefaultOptions({ locale: nb })
  useLastUmami()

  const dispatch = useAppDispatch()

  const [hentConfigStatus, hentConfig] = useApiCall(hentClientConfig)

  useEffect(() => {
    hentConfig({}, (res) => {
      dispatch(settAppversion(res.cachebuster))
    })
  }, [])

  const unleashUpdater = useUnleash()

  return mapResult(hentConfigStatus, {
    error: () => <ApiErrorAlert>Kunne ikke hente konfigurasjonsverdier</ApiErrorAlert>,
    success: (config) =>
      innloggetbrukerHentet && (
        <div className="app">
          <Unleashcontext.Provider value={unleashUpdater}>
            <Versioncheck />
            <BrowserRouter basename="/">
              <ScrollToTop />
              <ConfigContext.Provider value={config}>
                <HeaderBanner />
                <ErrorBoundary>
                  <Routes>
                    <Route path="/" element={<Oppgavebenk />} />
                    <Route path="/person" element={<Person />} />
                    <Route path="/person/sak/:sakId/brev/:brevId" element={<NyttBrev />} />
                    <Route path="/oppgave/:id/*" element={<BehandleJournalfoeringOppgave />} />
                    <Route path="/behandling/:behandlingId/*" element={<Behandling />} />
                    <Route path="/manuellbehandling/*" element={<ManuellBehandling />} />
                    <Route path="/generelloppgave/*" element={<GenerellOppgave />} />
                    <Route path="/klage/:klageId/*" element={<Klagebehandling />} />
                    <Route path="/tilbakekreving/:tilbakekrevingId/*" element={<Tilbakekrevingsbehandling />} />
                    <Route path="/generellbehandling/:generellbehandlingId" element={<GenerellBehandling />} />
                    <Route path="/aktivitet-vurdering/:oppgaveId/*" element={<VurderAktivitetspliktOppgave />} />
                    <Route path="/etteroppgjoer/:forbehandlingId/*" element={<EtteroppgjoerForbehandling />} />
                    <Route path="/svar-paa-etteroppgjoer/:oppgaveId/*" element={<SvarPaaEtteroppgjoer />} />
                    <Route path="/meldt-inn-endring/:oppgaveId/*" element={<MeldtInnEndring />} />
                    <Route path="*" element={<Navigate to="/" replace />} />
                  </Routes>
                </ErrorBoundary>
              </ConfigContext.Provider>
            </BrowserRouter>
          </Unleashcontext.Provider>
        </div>
      ),
  })
}

export default App
