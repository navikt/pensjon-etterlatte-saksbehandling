import { useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne/Beregne'
import { Vilkaarsvurdering } from './vilkaarsvurdering/Vilkaarsvurdering'
import { Soeknadsoversikt } from './soeknadsoversikt/Soeknadoversikt'
import Beregningsgrunnlag from './beregningsgrunnlag/Beregningsgrunnlag'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { useAppSelector } from '~store/Store'
import { Vedtaksbrev } from './vedtaksbrev/Vedtaksbrev'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { ManueltOpphoerOversikt } from './manueltopphoeroversikt/ManueltOpphoerOversikt'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Trygdetid } from '~components/behandling/trygdetid/Trygdetid'

const behandlingRoutes = [
  { path: 'soeknadsoversikt', element: <Soeknadsoversikt />, erRevurderingRoute: false, erManueltOpphoerRoute: false },
  {
    path: 'opphoeroversikt',
    element: <ManueltOpphoerOversikt />,
    erRevurderingRoute: false,
    erManueltOpphoerRoute: true,
  },
  {
    path: 'vilkaarsvurdering',
    element: <Vilkaarsvurdering />,
    erRevurderingRoute: true,
    erManueltOpphoerRoute: false,
  },
  { path: 'trygdetid', element: <Trygdetid />, erRevurderingRoute: false, erManueltOpphoerRoute: false },
  {
    path: 'beregningsgrunnlag',
    element: <Beregningsgrunnlag />,
    erRevurderingRoute: false,
    erManueltOpphoerRoute: false,
  },
  { path: 'beregne', element: <Beregne />, erRevurderingRoute: true, erManueltOpphoerRoute: true },
  { path: 'brev', element: <Vedtaksbrev />, erRevurderingRoute: true, erManueltOpphoerRoute: false },
] as const

function useRouteNavigation() {
  const [currentRoute, setCurrentRoute] = useState<string | undefined>()
  const navigate = useNavigate()
  const match = useMatch('/behandling/:behandlingId/:section')

  useEffect(() => {
    setCurrentRoute(match?.params?.section)
  }, [match])

  const goto = (path: (typeof behandlingRoutes)[number]['path']) => {
    setCurrentRoute(path)
    navigate(`/behandling/${match?.params?.behandlingId}/${path}`)
    window.scrollTo(0, 0)
  }

  return { currentRoute, goto }
}

export const useBehandlingRoutes = () => {
  const { currentRoute, goto } = useRouteNavigation()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const aktuelleRoutes = hentAktuelleRoutes(behandling)

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

  return { next, back, lastPage, firstPage, behandlingRoutes: aktuelleRoutes, currentRoute, goto }
}

const hentAktuelleRoutes = (behandling: IBehandlingReducer) => {
  switch (behandling.behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      const soeknadRoutes = behandlingRoutes.filter((route) => route.path !== 'opphoeroversikt')
      if (behandling.vilkårsprøving?.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT) {
        return soeknadRoutes.filter((route) => route.path !== 'beregne' && route.path !== 'beregningsgrunnlag')
      }
      return soeknadRoutes
    case IBehandlingsType.REVURDERING:
    case IBehandlingsType.OMREGNING:
      return behandlingRoutes.filter((route) => route.erRevurderingRoute)
    case IBehandlingsType.MANUELT_OPPHOER:
      return behandlingRoutes.filter((route) => route.erManueltOpphoerRoute)
  }
}
