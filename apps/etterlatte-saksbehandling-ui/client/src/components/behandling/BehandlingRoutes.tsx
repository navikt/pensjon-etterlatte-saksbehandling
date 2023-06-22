import { useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne/Beregne'
import { Vilkaarsvurdering } from './vilkaarsvurdering/Vilkaarsvurdering'
import { Soeknadsoversikt } from './soeknadsoversikt/Soeknadoversikt'
import Beregningsgrunnlag from './beregningsgrunnlag/Beregningsgrunnlag'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { useAppSelector } from '~store/Store'
import { Vedtaksbrev } from './brev/Vedtaksbrev'
import { ManueltOpphoerOversikt } from './manueltopphoeroversikt/ManueltOpphoerOversikt'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Revurderingsoversikt } from '~components/behandling/revurderingsoversikt/Revurderingsoversikt'
import { behandlingSkalSendeBrev } from '~components/behandling/felles/utils'

type behandlingRouteTypes =
  | 'soeknadsoversikt'
  | 'revurderingsoversikt'
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
  { path: 'revurderingsoversikt', element: <Revurderingsoversikt behandling={behandling} /> },
  { path: 'opphoeroversikt', element: <ManueltOpphoerOversikt behandling={behandling} /> },
  { path: 'vilkaarsvurdering', element: <Vilkaarsvurdering behandling={behandling} /> },
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

export interface BehandlingRouteTypes {
  path: string
  description: string
  kreverBehandlingsstatus?: IBehandlingStatus
}

export const soeknadRoutes: Array<BehandlingRouteTypes> = [
  { path: 'soeknadsoversikt', description: 'Søknadsoversikt' },
  {
    path: 'vilkaarsvurdering',
    description: 'Vilkårsvurdering',
    kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
  },
  {
    path: 'beregningsgrunnlag',
    description: 'Beregningsgrunnlag',
    kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
  },
  { path: 'beregne', description: 'Beregning', kreverBehandlingsstatus: IBehandlingStatus.BEREGNET },
  { path: 'brev', description: 'Vedtaksbrev' },
]

export const manueltOpphoerRoutes: Array<BehandlingRouteTypes> = [
  { path: 'opphoeroversikt', description: 'Opphøroversikt' },
  { path: 'beregne', description: 'Beregning', kreverBehandlingsstatus: IBehandlingStatus.BEREGNET },
]
const hentAktuelleRoutes = (behandling: IBehandlingReducer | null) => {
  if (!behandling) return []

  switch (behandling.behandlingType) {
    case IBehandlingsType.MANUELT_OPPHOER:
      return behandlingRoutes(behandling).filter((route) =>
        manueltOpphoerRoutes.map((pathinfo) => pathinfo.path).includes(route.path)
      )
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return behandlingRoutes(behandling).filter((route) =>
        soeknadRoutes.map((pathinfo) => pathinfo.path).includes(route.path)
      )
    case IBehandlingsType.REVURDERING:
      return behandlingRoutes(behandling).filter((route) =>
        revurderingRoutes(behandling)
          .map((pathinfo) => pathinfo.path)
          .includes(route.path)
      )
  }
}

export function revurderingRoutes(behandling: IBehandlingReducer): Array<BehandlingRouteTypes> {
  const defaultRoutes: Array<BehandlingRouteTypes> = [
    { path: 'revurderingsoversikt', description: 'Revurderingsoversikt' },
    {
      path: 'vilkaarsvurdering',
      description: 'Vilkårsvurdering',
      kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
    },
    {
      path: 'beregningsgrunnlag',
      description: 'Beregningsgrunnlag',
      kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
    },
    { path: 'beregne', description: 'Beregning', kreverBehandlingsstatus: IBehandlingStatus.BEREGNET },
  ]

  if (behandlingSkalSendeBrev(behandling)) {
    return [...defaultRoutes, { path: 'brev', description: 'Vedtaksbrev' }]
  }

  return defaultRoutes
}
