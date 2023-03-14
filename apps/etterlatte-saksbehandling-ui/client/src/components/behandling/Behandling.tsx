import { useEffect } from 'react'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { GridContainer, MainContent } from '~shared/styled'
import { addBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { StatusBar, StatusBarTheme } from '~shared/statusbar/Statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { SideMeny } from './SideMeny/SideMeny'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'

export const Behandling = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const dispatch = useAppDispatch()
  const match = useMatch('/behandling/:behandlingId/*')
  const { behandlingRoutes } = useBehandlingRoutes()
  const [hentBehandlingStatus, hentBehandlingCall] = useApiCall(hentBehandling)

  const behandlingId = behandling?.id

  useEffect(() => {
    const behandlingIdFraURL = match?.params.behandlingId
    if (!behandlingIdFraURL) return

    if (behandlingIdFraURL !== behandlingId) {
      hentBehandlingCall(
        behandlingIdFraURL,
        (res) => dispatch(addBehandling(res)),
        () => dispatch(resetBehandling())
      )
    }
  }, [match?.params.behandlingId, behandlingId])

  const soeker = behandling?.søker
  const soekerInfo = soeker
    ? { navn: `${soeker.fornavn} ${soeker.etternavn}`, fnr: soeker.foedselsnummer, type: 'Etterlatt' }
    : null

  return (
    <>
      {soekerInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={soekerInfo} />}
      {mapApiResult(
        hentBehandlingStatus,
        <Spinner visible label="Laster" />,
        () => (
          <>
            <StegMeny />
            <ApiErrorAlert>Kunne ikke hente behandling</ApiErrorAlert>
          </>
        ),
        (behandling) => (
          <>
            <StegMeny />
            <GridContainer>
              {behandling.id}
              <MainContent>
                <Routes>
                  {behandlingRoutes.map((route) => (
                    <Route key={route.path} path={route.path} element={route.element} />
                  ))}
                  <Route path="*" element={<Navigate to={behandlingRoutes[0].path} replace />} />
                </Routes>
              </MainContent>
              <SideMeny />
            </GridContainer>
          </>
        )
      )}
    </>
  )
}
