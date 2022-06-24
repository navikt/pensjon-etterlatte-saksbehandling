import { useContext, useEffect, useState } from 'react'
import { Routes, Route, useMatch } from 'react-router-dom'
import styled from 'styled-components'
import { hentBehandling } from '../../shared/api/behandling'
import { Column, GridContainer } from '../../shared/styled'
import { AppContext } from '../../store/AppContext'
import { IBehandlingsStatus } from './behandlings-status'
import { IApiResponse } from '../../shared/api/types'
import { IBehandlingStatus, IDetaljertBehandling } from '../../store/reducers/BehandlingReducer'
import Spinner from '../../shared/Spinner'
import { StatusBar, StatusBarTheme } from '../statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { IBehandlingInfo } from './SideMeny/types'
import { Attestering } from './attestering/attestering'
import { Behandlingsinfo } from './SideMeny/behandlingsinfo'
import { IRolle } from '../../store/reducers/SaksbehandlerReducer'

const addBehandlingAction = (data: any) => ({ type: 'add_behandling', data })

export const Behandling = () => {
  const ctx = useContext(AppContext)
  const match = useMatch('/behandling/:behandlingId/*')
  const { behandlingRoutes } = useBehandlingRoutes()
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
  const soekerInfo = soeker ? { navn: soeker.navn, foedselsnummer: soeker.fnr, type: 'Etterlatt' } : null

  const [behandlingsInfo, setBehandlingsinfo] = useState<IBehandlingInfo>()

  useEffect(() => {
    const behandling = ctx.state.behandlingReducer
    const innlogget = ctx.state.saksbehandlerReducer

    behandling &&
      setBehandlingsinfo({
        type: 'Førstegangsbehandling',
        status: behandling.status,
        saksbehandler: behandling.saksbehandlerId,
        attestant: behandling.attestant,
        virkningsdato: behandling.virkningstidspunkt,
        //vedtaksdato: '',
        rolle: innlogget.rolle,
      })
  }, [ctx.state])

  return (
    <>
      {soekerInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={soekerInfo} />}

      <Spinner visible={!loaded} label="Laster" />
      {loaded && (
        <GridContainer>
          <Column>
            <MenuHead>
              <Title>{IBehandlingsStatus.FORSTEGANG}</Title>
            </MenuHead>
            <StegMeny />
          </Column>
          <Column>
            <Routes>
              {behandlingRoutes.map((route) => {
                return <Route key={route.path} path={route.path} element={route.element} />
              })}
            </Routes>
          </Column>
          <Column>
            {behandlingsInfo && (
              <>
                {behandlingsInfo.rolle === IRolle.attestant &&
                behandlingsInfo.status === IBehandlingStatus.attestering ? (
                  <Attestering behandlingsInfo={behandlingsInfo} />
                ) : (
                  <Behandlingsinfo behandlingsInfo={behandlingsInfo} />
                )}
              </>
            )}
            {/*<Tab />*/}
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
