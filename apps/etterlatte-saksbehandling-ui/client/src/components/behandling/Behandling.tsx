import { useEffect } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { GridContainer, MainContent } from '~shared/styled'
import { setBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import { PdlPersonStatusBar } from '~shared/statusbar/Statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { useAppDispatch } from '~store/Store'
import { isSuccess, isPending, isFailure, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useBehandling } from '~components/behandling/useBehandling'
import { BehandlingSidemeny } from '~components/behandling/sidemeny/BehandlingSidemeny'
import Spinner from '~shared/Spinner'

export const Behandling = () => {
  const behandling = useBehandling()
  const dispatch = useAppDispatch()
  const { behandlingId: behandlingIdFraURL } = useParams()
  const { behandlingRoutes } = useBehandlingRoutes()
  const [fetchBehandlingStatus, fetchBehandling] = useApiCall(hentBehandling)

  useEffect(() => {
    if (!behandlingIdFraURL) {
      return
    }

    if (behandlingIdFraURL !== behandling?.id) {
      fetchBehandling(
        behandlingIdFraURL,
        (behandling) => dispatch(setBehandling(behandling)),
        () => dispatch(resetBehandling())
      )
    }
  }, [behandlingIdFraURL, behandling?.id])

  if (isPending(fetchBehandlingStatus)) {
    return <Spinner label="Henter behandling ..." visible />
  }
  if (isSuccess(fetchBehandlingStatus) && behandling != null) {
    return (
      <>
        {behandling.søker && <PdlPersonStatusBar person={behandling.søker} />}
        <StegMeny behandling={behandling} />
        <GridContainer>
          <MainContent>
            <Routes>
              {behandlingRoutes.map((route) => (
                <Route key={route.path} path={route.path} element={route.element} />
              ))}
              <Route path="*" element={<Navigate to={behandlingRoutes[0].path} replace />} />
            </Routes>
          </MainContent>
          <BehandlingSidemeny behandling={behandling} />
        </GridContainer>
      </>
    )
  }
  if (isFailure(fetchBehandlingStatus)) {
    return <ApiErrorAlert>Kunne ikke hente behandling</ApiErrorAlert>
  }
  return null
}
