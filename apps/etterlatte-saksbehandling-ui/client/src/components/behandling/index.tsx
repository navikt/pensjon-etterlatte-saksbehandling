import React, { useContext, useEffect, useState } from 'react'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import styled from 'styled-components'
import { hentBehandling } from '../../shared/api/behandling'
import { Column, GridContainer } from '../../shared/styled'
import { AppContext } from '../../store/AppContext'
import { IApiResponse } from '../../shared/api/types'
import { addBehandlingAction, IDetaljertBehandling } from '../../store/reducers/BehandlingReducer'
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
  const [loaded, setLoaded] = useState<boolean>(false)

  useEffect(() => {
    if (match?.params.behandlingId) {
      hentBehandling(match.params.behandlingId).then((response: IApiResponse<IDetaljertBehandling>) => {
        if (response.data) {
          ctx.dispatch(addBehandlingAction(response.data))
          setLoaded(true)
        }
      })
    }
  }, [match?.params.behandlingId])

  const soeker = ctx.state.behandlingReducer?.kommerSoekerTilgode?.familieforhold?.soeker
  const soekerInfo = soeker ? { navn: soeker.navn, fnr: soeker.fnr, type: 'Etterlatt' } : null
  const behandlingStatus = ctx.state.behandlingReducer?.status

  return (
    <>
      {soekerInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={soekerInfo} />}

      <Spinner visible={!loaded} label="Laster" />
      {loaded && (
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
