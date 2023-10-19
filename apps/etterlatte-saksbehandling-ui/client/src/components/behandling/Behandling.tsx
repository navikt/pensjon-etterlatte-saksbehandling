import React, { useEffect } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { GridContainer, MainContent } from '~shared/styled'
import { addBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import { PdlPersonStatusBar } from '~shared/statusbar/Statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { useAppDispatch } from '~store/Store'
import { isFailure, isInitial, isPending, mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useBehandling } from '~components/behandling/useBehandling'
import { BehandlingSidemeny } from '~components/behandling/sidemeny/BehandlingSidemeny'
import Spinner from '~shared/Spinner'
import { resetSjekkliste, updateSjekkliste } from '~store/reducers/SjekklisteReducer'
import { hentSjekkliste, opprettSjekkliste } from '~shared/api/sjekkliste'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'

export const Behandling = () => {
  const behandling = useBehandling()
  const dispatch = useAppDispatch()
  const { behandlingId: behandlingIdFraURL } = useParams()
  const { behandlingRoutes } = useBehandlingRoutes()
  const [fetchBehandlingStatus, fetchBehandling] = useApiCall(hentBehandling)

  const [hentSjekklisteResult, hentSjekklisteForBehandling] = useApiCall(hentSjekkliste)
  const [opprettSjekklisteResult, opprettSjekklisteForBehandling] = useApiCall(opprettSjekkliste)

  useEffect(() => {
    if (!behandlingIdFraURL) {
      return
    }

    if (behandlingIdFraURL !== behandling?.id) {
      fetchBehandling(
        behandlingIdFraURL,
        (behandling) => dispatch(addBehandling(behandling)),
        () => dispatch(resetBehandling())
      )
    }
  }, [behandlingIdFraURL, behandling?.id])

  useEffect(() => {
    dispatch(resetSjekkliste())
    if (behandling && isInitial(hentSjekklisteResult)) {
      hentSjekklisteForBehandling(
        behandling.id,
        (result) => {
          dispatch(updateSjekkliste(result))
        },
        () => {
          if (!erFerdigBehandlet(behandling.status)) {
            opprettSjekklisteForBehandling(behandling.id, (opprettet) => {
              dispatch(updateSjekkliste(opprettet))
            })
          }
        }
      )
    }
  }, [behandling])

  return mapAllApiResult(
    fetchBehandlingStatus,
    <Spinner label="Henter behandling ..." visible />,
    null,
    () => <ApiErrorAlert>Kunne ikke hente behandling</ApiErrorAlert>,
    () => {
      if (behandling) {
        return (
          <>
              {isPending(hentSjekklisteResult) && <Spinner label="Henter sjekkliste ..." visible />}
              {isFailure(hentSjekklisteResult) && !erFerdigBehandlet(behandling.status) && (
                  <ApiErrorAlert>En feil oppstod ved henting av sjekklista</ApiErrorAlert>
              )}
              {isFailure(opprettSjekklisteResult) && <ApiErrorAlert>Opprettelsen av sjekkliste feilet</ApiErrorAlert>}

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

              <BehandlingSidemeny />
            </GridContainer>
          </>
        )
      }
      return null
    }
  )
}
