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
import { Trygdetid } from './trygdetid/Trygdetid'
import { ISaksType } from '~components/behandling/fargetags/saksType'

type behandlingRouteTypes =
  | 'soeknadsoversikt'
  | 'opphoeroversikt'
  | 'vilkaarsvurdering'
  | 'trygdetid'
  | 'beregningsgrunnlag'
  | 'beregne'
  | 'brev'

const behandlingRoutes = (
  behandling: IBehandlingReducer
): Array<{ path: behandlingRouteTypes; element: JSX.Element }> => [
  { path: 'soeknadsoversikt', element: <Soeknadsoversikt behandling={behandling} /> },
  { path: 'opphoeroversikt', element: <ManueltOpphoerOversikt behandling={behandling} /> },
  { path: 'vilkaarsvurdering', element: <Vilkaarsvurdering behandling={behandling} /> },
  { path: 'trygdetid', element: <Trygdetid behandling={behandling} /> },
  { path: 'beregningsgrunnlag', element: <Beregningsgrunnlag behandling={behandling} /> },
  { path: 'beregne', element: <Beregne behandling={behandling} /> },
  { path: 'brev', element: <Vedtaksbrev behandling={behandling} /> },
]

function useRouteNavigation() {
  const [currentRoute, setCurrentRoute] = useState<string | undefined>()
  const navigate = useNavigate()
  const match = useMatch('/behandling/:behandlingId/:section')

  useEffect(() => {
    setCurrentRoute(match?.params?.section)
  }, [match])

  const goto = (path: behandlingRouteTypes) => {
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

const hentAktuelleRoutes = (behandling: IBehandlingReducer | null) => {
  if (!behandling) return []

  const soeknadRoutes = finnRoutesFoerstegangbehandling(behandling)
  const revurderingRoutes: Array<behandlingRouteTypes> = ['vilkaarsvurdering', 'beregne', 'brev']
  const manueltOpphoerRoutes: Array<behandlingRouteTypes> = ['opphoeroversikt', 'beregne']

  switch (behandling.behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return behandlingRoutes(behandling).filter((route) => soeknadRoutes.includes(route.path))
    case IBehandlingsType.REVURDERING:
    case IBehandlingsType.OMREGNING:
      return behandlingRoutes(behandling).filter((route) => revurderingRoutes.includes(route.path))
    case IBehandlingsType.MANUELT_OPPHOER:
      return behandlingRoutes(behandling).filter((route) => manueltOpphoerRoutes.includes(route.path))
  }
}

function finnRoutesFoerstegangbehandling(behandling: IBehandlingReducer): Array<behandlingRouteTypes> {
  const routes: Array<behandlingRouteTypes> = ['soeknadsoversikt', 'vilkaarsvurdering', 'brev']
  if (behandling.vilkårsprøving?.resultat?.utfall === VilkaarsvurderingResultat.OPPFYLT) {
    routes.push('beregningsgrunnlag')
    routes.push('beregne')
  }
  if (behandling.sakType == ISaksType.OMSTILLINGSSTOENAD) {
    routes.push('trygdetid')
  }
  return routes
}
