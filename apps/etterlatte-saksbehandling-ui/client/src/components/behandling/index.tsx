import { useContext, useEffect, useState } from 'react'
import { NavLink, Link, Routes, Route, useLocation, useMatch } from 'react-router-dom'
import styled from 'styled-components'
import { hentBehandling } from '../../shared/api/behandling'
import { ClockIcon } from '../../shared/icons/clockIcon'
import { DialogIcon } from '../../shared/icons/dialogIcon'
import { FolderIcon } from '../../shared/icons/folderIcon'
import { StatusIcon } from '../../shared/icons/statusIcon'
import { Column, GridContainer } from '../../shared/styled'
import { AppContext } from '../../store/AppContext'
import { BehandlingsStatus, IBehandlingsStatus } from './behandlings-status'
import { IApiResponse } from '../../shared/api/types'
import {
  IDetaljertBehandling,
  KildeType,
  OpplysningsType,
  VilkaarVurderingsResultat,
} from '../../store/reducers/BehandlingReducer'
import Spinner from '../../shared/Spinner'
import { StatusBar, StatusBarTheme } from '../statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'

const addBehandlingAction = (data: any) => ({ type: 'add_behandling', data })

export const Behandling = () => {
  const ctx = useContext(AppContext)
  const match = useMatch('/behandling/:behandlingId/*')
  const location = useLocation()
  const { behandlingRoutes } = useBehandlingRoutes()

  const [loaded, setLoaded] = useState<boolean>(false)

  useEffect(() => {
    if (match?.params.behandlingId) {
      hentBehandling(match.params.behandlingId).then((response: IApiResponse<IDetaljertBehandling>) => {
        ctx.dispatch(addBehandlingAction(response.data))
        setLoaded(false)
      })
    }
  }, [match?.params.behandlingId])

  const active = (hash: string) => {
    if (location.hash === hash) {
      return 'active'
    }
    return ''
  }

  const person: any = ctx.state.behandlingReducer?.grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.pdl
  )?.opplysning

  return (
    <>
      <StatusBar theme={StatusBarTheme.gray} personInfo={person} />
      <SpinnerWrap>
        <Spinner visible={!loaded} label="Laster" />
      </SpinnerWrap>
      {loaded && (
        <GridContainer>
          <Column>
            <MenuHead>
              <BehandlingsStatus status={IBehandlingsStatus.FORSTEGANG} />
            </MenuHead>
            {/* stegmeny */}
            <StegMeny>
              <li>
                <NavLink to="soeknadsoversikt">
                  <span>1.</span> Søknadsoversikt
                </NavLink>
              </li>
              <li>
                {/** Innholdet her dras ut i egen komponent og vil være dynamisk basert på hvilke vilkår som faktisk eksisterer */}
                <NavLink to="inngangsvilkaar">
                  <span>2.</span> Inngangsvilkår
                </NavLink>
                <CheckedMenu>
                  <li>
                    <Link to="#alderbarn" className={active('#alderbarn')}>
                      <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} />
                      <span>Alder</span>
                    </Link>
                  </li>
                  <li>
                    <Link to="#dodsfallforelder" className={active('#dodsfallforelder')}>
                      <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} />
                      <span>Dødsfall</span>
                    </Link>
                  </li>
                  <li>
                    <Link to="#avdodesmedlemskap" className={active('#avdodesmedlemskap')}>
                      <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} />
                      <span>Medlemsskap</span>
                    </Link>
                  </li>
                  <li>
                    <Link to="#bostedsadresse" className={active('#bostedsadresse')}>
                      <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} />
                      <span>Bostedsadresse</span>
                    </Link>
                  </li>

                  <li>
                    <Link to="#yrkesskade" className={active('#yrkesskade')}>
                      <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} />
                      <span>Yrkesskade</span>
                    </Link>
                  </li>
                </CheckedMenu>
              </li>
              <li>
                <NavLink to="beregne">
                  <span>3.</span> Beregne
                </NavLink>
              </li>
              <li>
                <NavLink to="vedtak">
                  <span>4.</span> Vedtak
                </NavLink>
              </li>
              <li>
                <NavLink to="utbetalingsoversikt">
                  <span>5.</span> Utbetalingsoversikt
                </NavLink>
              </li>
              <li>
                <NavLink to="brev">
                  <span>6.</span> Brev
                </NavLink>
              </li>
            </StegMeny>
            {/* Subroutes for stegmeny feks */}
          </Column>
          <Column>
            <Routes>
              {behandlingRoutes.map((route) => {
                return <Route key={route.path} path={route.path} element={route.element} />
              })}
            </Routes>
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

const StegMeny = styled.ul`
  height: 100px;
  list-style: none;
  padding: 1em 0em 0;

  li {
    a {
      display: block;
      padding: 0.5em 1em 0.5em;
      color: #78706a;
      text-decoration: none;
      border-bottom: 3px solid transparent;
      border-left: 6px solid transparent;
      &:hover {
        text-decoration: underline;
      }
      &.active {
        border-left: 6px solid #0067c5;
        background-color: #e7e9e9;
      }
    }
  }
`

const MenuHead = styled.div`
  padding: 2em 1em;
  height: 100px;
  border-bottom: 1px solid #ccc;
`

const CheckedMenu = styled.ul`
  margin: 1em 0 0;

  li {
    display: flex;
    flex-wrap: wrap;
    padding: 0 4em;
    &:after {
      height: 25px;
      width: 100%;
      content: '';
      border-left: 1px solid #000;
      margin: 5px 0 8px 10px;
    }
    &:last-child:after {
      border: none;
      height: 0;
    }

    &.selected {
      opacity: 1;
    }

    a {
      opacity: 0.4;
      border: none !important;
      display: inherit;
      font-weight: inherit;
      color: inherit;
      padding: 0;
      &.active {
        background: none;
        opacity: 1;
      }
    }

    span {
      padding: 0 0.5em;
    }
  }
`

const SpinnerWrap = styled.div`
  display: flex;
  justify-content:center;
  margin: 3em;
  text-align: center;
`;