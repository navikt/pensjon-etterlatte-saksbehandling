import { useEffect } from 'react'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { GridContainer, MainContent } from '~shared/styled'
import {
  addBehandling,
  resetBehandling,
  updateVilkaarsvurdering,
  updateVedtakSammendrag,
} from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { SideMeny } from './SideMeny/SideMeny'
import { useAppDispatch } from '~store/Store'
import { isFailure, isPendingOrInitial, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { useBehandling } from '~components/behandling/useBehandling'

export const Behandling = () => {
  const behandling = useBehandling()
  const dispatch = useAppDispatch()
  const match = useMatch('/behandling/:behandlingId/*')
  const { behandlingRoutes } = useBehandlingRoutes()
  const [fetchBehandlingStatus, fetchBehandling] = useApiCall(hentBehandling)
  const [fetchVilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)

  const behandlingId = behandling?.id

  useEffect(() => {
    const behandlingIdFraURL = match?.params.behandlingId
    if (!behandlingIdFraURL) return

    if (behandlingIdFraURL !== behandlingId) {
      fetchBehandling(
        behandlingIdFraURL,
        (res) => {
          dispatch(addBehandling(res))

          fetchVilkaarsvurdering(behandlingIdFraURL, (vilkaarsvurdering) => {
            if (vilkaarsvurdering !== null) {
              dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
            }
          })

          fetchVedtakSammendrag(behandlingIdFraURL, (vedtakSammendrag) => {
            if (vedtakSammendrag !== null) {
              dispatch(updateVedtakSammendrag(vedtakSammendrag))
            }
          })
        },
        () => dispatch(resetBehandling())
      )
    }
  }, [match?.params.behandlingId, behandlingId])

  const soekerInfo = behandling?.søker

  return (
    <>
      {soekerInfo && (
        <StatusBar
          result={{
            status: 'success',
            data: {
              foedselsnummer: soekerInfo.foedselsnummer,
              fornavn: soekerInfo.fornavn,
              mellomnavn: soekerInfo.mellomnavn,
              etternavn: soekerInfo.etternavn,
            },
          }}
        />
      )}

      {behandling && <StegMeny behandling={behandling} />}

      <Spinner
        visible={
          isPendingOrInitial(fetchBehandlingStatus) ||
          isPendingOrInitial(fetchVilkaarsvurderingStatus) ||
          isPendingOrInitial(fetchVedtakStatus)
        }
        label="Laster"
      />
      {behandling && (
        <GridContainer>
          <MainContent>
            <Routes>
              {behandlingRoutes.map((route) => (
                <Route key={route.path} path={route.path} element={route.element} />
              ))}
              <Route path="*" element={<Navigate to={behandlingRoutes[0].path} replace />} />
            </Routes>
          </MainContent>
          <SideMeny behandling={behandling} vedtak={behandling.vedtak} />
        </GridContainer>
      )}
      {isFailure(fetchBehandlingStatus) && <ApiErrorAlert>Kunne ikke hente behandling</ApiErrorAlert>}
      {isFailure(fetchVedtakStatus) && <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>}
    </>
  )
}
