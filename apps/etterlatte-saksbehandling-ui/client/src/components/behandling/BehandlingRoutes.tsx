import { useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne/Beregne'
import { Vilkaarsvurdering } from './vilkaarsvurdering/Vilkaarsvurdering'
import { Soeknadsoversikt } from './soeknadsoversikt/Soeknadoversikt'
import Beregningsgrunnlag from './beregningsgrunnlag/Beregningsgrunnlag'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { Vedtaksbrev } from './brev/Vedtaksbrev'
import { ManueltOpphoerOversikt } from './manueltopphoeroversikt/ManueltOpphoerOversikt'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Revurderingsoversikt } from '~components/behandling/revurderingsoversikt/Revurderingsoversikt'
import { behandlingSkalSendeBrev } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { erOpphoer, Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { Aktivitetsplikt } from '~components/behandling/aktivitetsplikt/Aktivitetsplikt'
import { SakType } from '~shared/types/sak'
import TrygdetidVisning from '~components/behandling/trygdetid/TrygdetidVisning'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'

type behandlingRouteTypes =
  | 'soeknadsoversikt'
  | 'revurderingsoversikt'
  | 'opphoeroversikt'
  | 'vilkaarsvurdering'
  | 'trygdetid'
  | 'beregningsgrunnlag'
  | 'aktivitetsplikt'
  | 'beregne'
  | 'brev'

const behandlingRoutes = (
  behandling: IBehandlingReducer
): Array<{ path: behandlingRouteTypes; element: JSX.Element }> => [
  { path: 'soeknadsoversikt', element: <Soeknadsoversikt behandling={behandling} /> },
  { path: 'revurderingsoversikt', element: <Revurderingsoversikt behandling={behandling} /> },
  { path: 'opphoeroversikt', element: <ManueltOpphoerOversikt behandling={behandling} /> },
  { path: 'vilkaarsvurdering', element: <Vilkaarsvurdering behandling={behandling} /> },
  { path: 'trygdetid', element: <TrygdetidVisning behandling={behandling} /> },
  { path: 'beregningsgrunnlag', element: <Beregningsgrunnlag behandling={behandling} /> },
  { path: 'aktivitetsplikt', element: <Aktivitetsplikt behandling={behandling} /> },
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
  const behandling = useBehandling()
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
  sakstype?: SakType
}

export function soeknadRoutes(behandling: IBehandlingReducer): Array<BehandlingRouteTypes> {
  const soeknadsoversikt = { path: 'soeknadsoversikt', description: 'Søknadsoversikt' }
  const vilkaarsvurdering = {
    path: 'vilkaarsvurdering',
    description: 'Vilkårsvurdering',
    kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
  }
  const trygdetid = {
    path: 'trygdetid',
    description: 'Trygdetid',
    kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
  }
  const beregningsgrunnlag = {
    path: 'beregningsgrunnlag',
    description: 'Beregningsgrunnlag',
    kreverBehandlingsstatus: IBehandlingStatus.TRYGDETID_OPPDATERT,
  }
  const aktivitetsplikt = {
    path: 'aktivitetsplikt',
    description: 'Oppfølging av aktivitet',
    kreverBehandlingsstatus: IBehandlingStatus.TRYGDETID_OPPDATERT,
    sakstype: SakType.OMSTILLINGSSTOENAD,
  }

  const beregning = {
    path: 'beregne',
    description: 'Beregning',
    kreverBehandlingsstatus: IBehandlingStatus.BEREGNET,
  }
  const brev = {
    path: 'brev',
    description: 'Vedtaksbrev',
    kreverBehandlingsstatus:
      behandling.sakType == SakType.OMSTILLINGSSTOENAD ? IBehandlingStatus.AVKORTET : IBehandlingStatus.BEREGNET,
  }

  const erAvslag = behandling.vilkårsprøving?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  const defaultRoutes: Array<BehandlingRouteTypes> = erAvslag
    ? [soeknadsoversikt, vilkaarsvurdering]
    : [soeknadsoversikt, vilkaarsvurdering, trygdetid, beregningsgrunnlag, aktivitetsplikt, beregning]

  const defaultRoutesForYtelse = defaultRoutes.filter(
    (route) => route.sakstype === undefined || route.sakstype === behandling.sakType
  )

  if (behandlingSkalSendeBrev(behandling.behandlingType, behandling.revurderingsaarsak)) {
    return [...defaultRoutesForYtelse, brev]
  }

  return defaultRoutesForYtelse
}

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
        soeknadRoutes(behandling)
          .map((pathinfo) => pathinfo.path)
          .includes(route.path)
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
  const revurderingsoversikt = { path: 'revurderingsoversikt', description: 'Revurderingsoversikt' }
  const vilkaarsvurdering = {
    path: 'vilkaarsvurdering',
    description: 'Vilkårsvurdering',
    kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
  }
  const trygdetid = {
    path: 'trygdetid',
    description: 'Trygdetid',
    kreverBehandlingsstatus: IBehandlingStatus.VILKAARSVURDERT,
  }
  const beregningsgrunnlag = {
    path: 'beregningsgrunnlag',
    description: 'Beregningsgrunnlag',
    kreverBehandlingsstatus: IBehandlingStatus.TRYGDETID_OPPDATERT,
  }
  const aktivitetsplikt = {
    path: 'aktivitetsplikt',
    description: 'Oppfølging av aktivitet',
    kreverBehandlingsstatus: IBehandlingStatus.TRYGDETID_OPPDATERT,
    sakstype: SakType.OMSTILLINGSSTOENAD,
  }
  const beregning = {
    path: 'beregne',
    description: 'Beregning',
    kreverBehandlingsstatus: IBehandlingStatus.BEREGNET,
  }
  const brev = {
    path: 'brev',
    description: 'Vedtaksbrev',
    kreverBehandlingsstatus:
      behandling.sakType == SakType.OMSTILLINGSSTOENAD ? IBehandlingStatus.AVKORTET : IBehandlingStatus.BEREGNET,
  }

  const erRevurderingsaarsakAnnenOgOpphoer =
    behandling.revurderingsaarsak == Revurderingsaarsak.ANNEN &&
    behandling.vilkårsprøving?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  const defaultRoutes: Array<BehandlingRouteTypes> =
    erOpphoer(behandling.revurderingsaarsak!!) || erRevurderingsaarsakAnnenOgOpphoer
      ? [revurderingsoversikt, vilkaarsvurdering, beregning]
      : [revurderingsoversikt, vilkaarsvurdering, trygdetid, beregningsgrunnlag, aktivitetsplikt, beregning]

  const defaultRoutesForYtelse = defaultRoutes.filter(
    (route) => route.sakstype === undefined || route.sakstype === behandling.sakType
  )

  if (behandlingSkalSendeBrev(behandling.behandlingType, behandling.revurderingsaarsak)) {
    return [...defaultRoutesForYtelse, brev]
  }

  return defaultRoutesForYtelse
}
