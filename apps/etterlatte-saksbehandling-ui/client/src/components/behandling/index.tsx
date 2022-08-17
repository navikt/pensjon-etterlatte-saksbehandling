import { useContext, useEffect, useState } from 'react'
import { Routes, Route, useMatch } from 'react-router-dom'
import styled from 'styled-components'
import { hentBehandling } from '../../shared/api/behandling'
import { Column, GridContainer } from '../../shared/styled'
import { AppContext } from '../../store/AppContext'
import { IBehandlingsType } from './behandlingsType'
import { IApiResponse } from '../../shared/api/types'
import { IDetaljertBehandling } from '../../store/reducers/BehandlingReducer'
import Spinner from '../../shared/Spinner'
import { StatusBar, StatusBarTheme } from '../statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { SideMeny } from './SideMeny'

const addBehandlingAction = (data: any) => (
  {type: 'add_behandling', data}
)

export const Behandling = () => {
  const ctx = useContext(AppContext)
  const match = useMatch('/behandling/:behandlingId/*')
  const {behandlingRoutes} = useBehandlingRoutes()
  const [loaded, setLoaded] = useState<boolean>(false)

  useEffect(() => {
    if (match?.params.behandlingId) {
      hentBehandling(match.params.behandlingId).then((response: IApiResponse<IDetaljertBehandling>) => {
        ctx.dispatch(addBehandlingAction(response.data))
        setLoaded(true)
      })
    }
  }, [match?.params.behandlingId])

  const soeker = ctx.state.behandlingReducer?.kommerSoekerTilgode?.familieforhold.soeker
  const soekerInfo = soeker ? {navn: soeker.navn, fnr: soeker.fnr, type: 'Etterlatt'} : null

  return (
    <>
      {soekerInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={soekerInfo}/>}

      <Spinner visible={!loaded} label="Laster"/>
      {loaded && (
        <GridContainer>
          <Column>
            <MenuHead>
              <Title>{IBehandlingsType.FØRSTEGANGSBEHANDLING}</Title>
            </MenuHead>
            <StegMeny/>
          </Column>
          <Column>
            <Routes>
              {behandlingRoutes.map((route) => {
                return <Route key={route.path} path={route.path} element={route.element}/>
              })}
            </Routes>
          </Column>
          <Column>
            <SideMeny/>
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
