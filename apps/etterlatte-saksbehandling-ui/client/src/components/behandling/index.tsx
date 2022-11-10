import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { GridContainer, MainContent } from '~shared/styled'
import { addBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { StatusBar, StatusBarTheme } from '~shared/statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { SideMeny } from './SideMeny'
import { useAppDispatch, useAppSelector } from '~store/Store'

export const Behandling = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const dispatch = useAppDispatch()
  const match = useMatch('/behandling/:behandlingId/*')
  const { behandlingRoutes } = useBehandlingRoutes()
  const behandlingId = behandling.id
  const [isLoading, setIsLoading] = useState<boolean>(behandlingId === '')

  useEffect(() => {
    const behandlingIdFraURL = match?.params.behandlingId
    if (!behandlingIdFraURL) return

    if (behandlingIdFraURL !== behandlingId) {
      fetchBehandling(behandlingIdFraURL)
    }

    async function fetchBehandling(behandlingId: string) {
      setIsLoading(true)
      const response = await hentBehandling(behandlingId)

      if (response.status === 'ok') {
        dispatch(addBehandling(response.data))
      } else {
        dispatch(resetBehandling())
      }
      setIsLoading(false)
    }
  }, [match?.params.behandlingId, behandlingId])

  const soeker = behandling?.søker
  const soekerInfo = soeker
    ? { navn: `${soeker.fornavn} ${soeker.etternavn}`, fnr: soeker.foedselsnummer, type: 'Etterlatt' }
    : null

  return (
    <>
      {soekerInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={soekerInfo} />}
      {!isLoading && <StegMeny />}

      <Spinner visible={isLoading} label="Laster" />
      {!isLoading && (
        <GridContainer>
          <MainContent>
            <Routes>
              {behandlingRoutes.map((route) => {
                return <Route key={route.path} path={route.path} element={route.element} />
              })}
              <Route path="*" element={<Navigate to={behandlingRoutes[0].path} replace />} />
            </Routes>
          </MainContent>
          <SideMeny />
        </GridContainer>
      )}
    </>
  )
}
