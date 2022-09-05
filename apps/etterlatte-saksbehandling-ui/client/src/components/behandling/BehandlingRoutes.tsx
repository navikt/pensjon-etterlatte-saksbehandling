import { useContext, useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne'
import { Inngangsvilkaar } from './inngangsvilkaar'
import { Soeknadsoversikt } from './soeknadsoversikt'
import { Brev } from './brev'
import Beregningsgrunnlag from './beregningsgrunnlag'
import { IBehandlingsType } from '../../store/reducers/BehandlingReducer'
import { AppContext } from '../../store/AppContext'
// import { Utbetalingsoversikt } from './utbetalingsoversikt'
// import { Vedtak } from './vedtak'

const behandlingRoutes = [
  { path: 'soeknadsoversikt', element: <Soeknadsoversikt />, erRevurderingRoute: false },
  { path: 'inngangsvilkaar', element: <Inngangsvilkaar />, erRevurderingRoute: true },
  { path: 'beregningsgrunnlag', element: <Beregningsgrunnlag />, erRevurderingRoute: false },
  { path: 'beregne', element: <Beregne />, erRevurderingRoute: true },
  // { path: 'vedtak', element: <Vedtak /> },
  // { path: 'utbetalingsoversikt', element: <Utbetalingsoversikt /> },
  { path: 'brev', element: <Brev />, erRevurderingRoute: true },
] as const

const revurderingBehandlingRoutes = behandlingRoutes.filter((route) => route.erRevurderingRoute)
const foerstegangBehandlingRoutes = behandlingRoutes

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
  const { state } = useContext(AppContext)
  const aktuelleRoutes =
    state.behandlingReducer?.behandlingType === IBehandlingsType.REVURDERING
      ? revurderingBehandlingRoutes
      : foerstegangBehandlingRoutes

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
