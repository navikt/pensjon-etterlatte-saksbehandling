import { useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne/Beregne'
import { Vilkaarsvurdering } from './vilkaarsvurdering/Vilkaarsvurdering'
import { Soeknadsoversikt } from './soeknadsoversikt/Soeknadoversikt'
import Beregningsgrunnlag from './beregningsgrunnlag/Beregningsgrunnlag'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { useAppSelector } from '~store/Store'
import { Vedtaksbrev } from './vedtaksbrev/Vedtaksbrev'

const behandlingRoutes = [
  { path: 'soeknadsoversikt', element: <Soeknadsoversikt />, erRevurderingRoute: false },
  { path: 'vilkaarsvurdering', element: <Vilkaarsvurdering />, erRevurderingRoute: true },
  { path: 'beregningsgrunnlag', element: <Beregningsgrunnlag />, erRevurderingRoute: false },
  { path: 'beregne', element: <Beregne />, erRevurderingRoute: true },
  { path: 'brev', element: <Vedtaksbrev />, erRevurderingRoute: true },
] as const

const revurderingBehandlingRoutes = () => behandlingRoutes.filter((route) => route.erRevurderingRoute)

function useRouteNavigation() {
  const [currentRoute, setCurrentRoute] = useState<string | undefined>()
  const navigate = useNavigate()
  const match = useMatch('/behandling/:behandlingId/:section')

  useEffect(() => {
    setCurrentRoute(match?.params?.section)
  }, [match])

  const goto = (path: typeof behandlingRoutes[number]['path']) => {
    setCurrentRoute(path)
    navigate(`/behandling/${match?.params?.behandlingId}/${path}`)
    window.scrollTo(0, 0)
  }

  return { currentRoute, goto }
}

export const useBehandlingRoutes = () => {
  const { currentRoute, goto } = useRouteNavigation()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const aktuelleRoutes =
    behandling?.behandlingType === IBehandlingsType.REVURDERING ||
    behandling?.behandlingType === IBehandlingsType.MANUELT_OPPHOER
      ? revurderingBehandlingRoutes()
      : behandlingRoutes

  const firstPage = aktuelleRoutes.findIndex((item) => item.path === currentRoute) === 0
  const lastPage = aktuelleRoutes.findIndex((item) => item.path === currentRoute) === aktuelleRoutes.length - 1

  const next = () => {
    const index = aktuelleRoutes.findIndex((item) => item.path === currentRoute)
    const nextPath = aktuelleRoutes[index + 1].path
    goto(nextPath)
  }

  const back = () => {
    const index = aktuelleRoutes.findIndex((item) => item.path === currentRoute)
    const previousPath = aktuelleRoutes[index - 1].path
    goto(previousPath)
  }

  return { next, back, lastPage, firstPage, behandlingRoutes: aktuelleRoutes, currentRoute }
}
