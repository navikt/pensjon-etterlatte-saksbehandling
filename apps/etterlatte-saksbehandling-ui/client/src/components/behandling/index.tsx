import { useContext, useEffect, useState } from 'react'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import styled from 'styled-components'
import { hentBehandling } from '../../shared/api/behandling'
import { Column, GridContainer } from '../../shared/styled'
import { AppContext } from '../../store/AppContext'
import { addBehandlingAction, resetBehandlingAction } from '../../store/reducers/BehandlingReducer'
import Spinner from '../../shared/Spinner'
import { StatusBar, StatusBarTheme } from '../../shared/statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { SideMeny } from './SideMeny'
import { formaterEnumTilLesbarString } from '../../utils/formattering'
import { RevurderingsAarsakModal } from './inngangsvilkaar/revurderingInfo/RevurderingInfoModal'

export const Behandling = () => {
  const ctx = useContext(AppContext)
  const match = useMatch('/behandling/:behandlingId/*')
  const { behandlingRoutes } = useBehandlingRoutes()
  const behandlingId = ctx.state.behandlingReducer?.id
  const [isLoading, setIsLoading] = useState<boolean>(behandlingId === undefined)

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
        ctx.dispatch(addBehandlingAction(response.data))
      } else {
        ctx.dispatch(resetBehandlingAction())
      }
      setIsLoading(false)
    }
  }, [match?.params.behandlingId, behandlingId])

  const soeker = ctx.state.behandlingReducer?.kommerSoekerTilgode?.familieforhold?.soeker
  const soekerInfo = soeker ? { navn: soeker.navn, fnr: soeker.fnr, type: 'Etterlatt' } : null
  const behandlingStatus = ctx.state.behandlingReducer?.status

  return (
    <>
      {soekerInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={soekerInfo} />}

      <Spinner visible={isLoading} label="Laster" />
      {!isLoading && (
        <GridContainer>
          <Column>
            <MenuHead>
              <Title>{formaterEnumTilLesbarString(ctx.state.behandlingReducer.behandlingType)}</Title>
            </MenuHead>
            <StegMeny />
          </Column>
          <RevurderingsAarsakModal behandlingStatus={behandlingStatus} />
          <Column>
            <Routes>
              {behandlingRoutes.map((route) => {
                return <Route key={route.path} path={route.path} element={route.element} />
              })}
              <Route path="*" element={<Navigate to={behandlingRoutes[0].path} replace />} />
            </Routes>
          </Column>
          <Column>
            <SideMeny />
          </Column>
        </GridContainer>
      )}
    </>
  )
}

const MenuHead = styled.div`
  padding: 2em 1em;
  height: 100px;
`
const Title = styled.div`
  font-weight: 600;
  font-size: 24px;
`
