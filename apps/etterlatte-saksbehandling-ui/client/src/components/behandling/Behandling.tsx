import { useEffect } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { GridContainer, MainContent } from '~shared/styled'
import { IBehandlingReducer, resetBehandling, setBehandling } from '~store/reducers/BehandlingReducer'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { BehandlingRouteContext, useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useBehandling } from '~components/behandling/useBehandling'
import { BehandlingSidemeny } from '~components/behandling/sidemeny/BehandlingSidemeny'
import Spinner from '~shared/Spinner'
import { hentPersonopplysningerForBehandling } from '~shared/api/grunnlag'
import { resetPersonopplysninger, setPersonopplysninger } from '~store/reducers/PersonopplysningerReducer'
import { mapResult } from '~shared/api/apiUtils'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { StickyToppMeny } from '~shared/StickyToppMeny'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

export const Behandling = () => {
  useSidetittel('Behandling')

  const behandlingFraRedux = useBehandling()
  const dispatch = useAppDispatch()
  const { behandlingId: behandlingIdFraURL } = useParams()
  const routedata = useBehandlingRoutes()
  const [fetchBehandlingStatus, fetchBehandling] = useApiCall(hentBehandling)
  const [, fetchPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)
  const soeker = usePersonopplysninger()?.soeker?.opplysning

  useEffect(() => {
    if (!behandlingIdFraURL) {
      return
    }

    if (behandlingIdFraURL !== behandlingFraRedux?.id) {
      fetchBehandling(
        behandlingIdFraURL,
        (behandling) => {
          if (behandling.hendelser) {
            console.log(behandling.hendelser)
          }
          dispatch(setBehandling(behandling))
        },
        () => dispatch(resetBehandling())
      )
    }
  }, [behandlingIdFraURL, behandlingFraRedux?.id])

  useEffect(() => {
    if (behandlingFraRedux !== null) {
      fetchPersonopplysninger(
        { behandlingId: behandlingFraRedux.id, sakType: behandlingFraRedux.sakType },
        (result) => dispatch(setPersonopplysninger(result)),
        () => dispatch(resetPersonopplysninger())
      )
    } else {
      dispatch(resetPersonopplysninger())
    }
  }, [behandlingFraRedux])

  return mapResult(fetchBehandlingStatus, {
    pending: <Spinner label="Henter behandling ..." />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente behandling: {error.detail}</ApiErrorAlert>,
    success: () => {
      if (behandlingFraRedux) {
        const behandling = behandlingFraRedux as IBehandlingReducer
        return (
          <BehandlingRouteContext.Provider value={routedata}>
            <StickyToppMeny>
              <StatusBar ident={soeker?.foedselsnummer} />
              <StegMeny behandling={behandling} />
            </StickyToppMeny>
            <GridContainer>
              <MainContent>
                <Routes>
                  {routedata.behandlingRoutes.map((route) => (
                    <Route key={route.path} path={route.path} element={route.element(behandling)} />
                  ))}
                  <Route path="*" element={<Navigate to={routedata.behandlingRoutes[0].path} replace />} />
                </Routes>
              </MainContent>
              <BehandlingSidemeny behandling={behandling} />
            </GridContainer>
          </BehandlingRouteContext.Provider>
        )
      }
      return null
    },
  })
}
