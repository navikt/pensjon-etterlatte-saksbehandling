import { useContext, useEffect, useState } from 'react'
import { Routes, Route, useMatch } from 'react-router-dom'
import styled from 'styled-components'
import { hentBehandling } from '../../shared/api/behandling'
import { ClockIcon } from '../../shared/icons/clockIcon'
import { DialogIcon } from '../../shared/icons/dialogIcon'
import { FolderIcon } from '../../shared/icons/folderIcon'
import { Column, GridContainer } from '../../shared/styled'
import { AppContext } from '../../store/AppContext'
import { BehandlingsStatus, IBehandlingsStatus } from './behandlings-status'
import { IApiResponse } from '../../shared/api/types'
import { IDetaljertBehandling, OpplysningsType } from '../../store/reducers/BehandlingReducer'
import Spinner from '../../shared/Spinner'
import { StatusBar, StatusBarTheme } from '../statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { BehandlingHandlingKnapper } from './handlinger/BehandlingHandlingKnapper'
import { StegMeny } from './StegMeny/stegmeny'

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

  const person: any = ctx.state.behandlingReducer?.grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_pdl
  )?.opplysning

  return (
    <>
      <StatusBar theme={StatusBarTheme.gray} personInfo={person} />
      <Spinner visible={!loaded} label="Laster" />
      {loaded && (
        <GridContainer>
          <Column>
            <MenuHead>
              <BehandlingsStatus status={IBehandlingsStatus.FORSTEGANG} />
            </MenuHead>
            <StegMeny />
          </Column>
          <Column>
            <Routes>
              {behandlingRoutes.map((route) => {
                return <Route key={route.path} path={route.path} element={route.element} />
              })}
            </Routes>
            {loaded && <BehandlingHandlingKnapper />}
          </Column>
          <Column>
            <Tab />
          </Column>
        </GridContainer>
      )}
    </>
  )
}

const Tab = () => {
  const [selected, setSelected] = useState('1')

  const select = (e: any) => {
    setSelected(e.currentTarget.dataset.value)
  }

  const isSelectedClass = (val: string): string => {
    if (selected === val) {
      return 'active'
    }
    return ''
  }

  const renderSubElement = () => {
    switch (selected) {
      case '1':
        return <div>Hei</div>
      case '2':
        return <div>på</div>
      case '3':
        return <div>deg</div>
    }
    return
  }

  return (
    <>
      <MenuHead style={{ paddingTop: '14px', paddingLeft: 0, paddingRight: 0 }}>
        <Tabs>
          <IconButton data-value="1" onClick={select} className={isSelectedClass('1')}>
            <ClockIcon />
          </IconButton>
          <IconButton data-value="2" onClick={select} className={isSelectedClass('2')}>
            <DialogIcon />
          </IconButton>
          <IconButton data-value="3" onClick={select} className={isSelectedClass('3')}>
            <FolderIcon />
          </IconButton>
        </Tabs>
      </MenuHead>
      {renderSubElement()}
    </>
  )
}

const Tabs = styled.div`
  display: flex;

  > div {
    width: 33.333%;
    height: 100%;
    justify-content: center;
    align-items: center;
    text-align: center;
  }
`
const IconButton = styled.div`
  cursor: pointer;
  padding: 1em 1em 1.8em;

  &.active {
    border-bottom: 3px solid #0067c5;
  }
`

const MenuHead = styled.div`
  padding: 2em 1em;
  height: 100px;
  border-bottom: 1px solid #ccc;
`
