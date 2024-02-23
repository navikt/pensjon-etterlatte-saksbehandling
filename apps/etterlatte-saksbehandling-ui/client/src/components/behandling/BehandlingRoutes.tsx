import React, { useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne/Beregne'
import { Vilkaarsvurdering } from './vilkaarsvurdering/Vilkaarsvurdering'
import { Soeknadsoversikt } from './soeknadsoversikt/Soeknadoversikt'
import Beregningsgrunnlag from './beregningsgrunnlag/Beregningsgrunnlag'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IDetaljertBehandling,
  Vedtaksloesning,
} from '~shared/types/IDetaljertBehandling'
import { Vedtaksbrev } from './brev/Vedtaksbrev'
import { ManueltOpphoerOversikt } from './manueltopphoeroversikt/ManueltOpphoerOversikt'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Revurderingsoversikt } from '~components/behandling/revurderingsoversikt/Revurderingsoversikt'
import { behandlingSkalSendeBrev, soeknadsoversiktErFerdigUtfylt } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { Aktivitetsplikt } from '~components/behandling/aktivitetsplikt/Aktivitetsplikt'
import { SakType } from '~shared/types/sak'
import TrygdetidVisning from '~components/behandling/trygdetid/TrygdetidVisning'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { erOpphoer } from '~shared/types/Revurderingaarsak'
import { FEATURE_TOGGLE_LAG_VARSELBREV, Varselbrev } from '~components/behandling/brev/Varselbrev'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

type behandlingRouteTypes =
  | 'soeknadsoversikt'
  | 'revurderingsoversikt'
  | 'opphoeroversikt'
  | 'vilkaarsvurdering'
  | 'aktivitetsplikt'
  | 'trygdetid'
  | 'beregningsgrunnlag'
  | 'beregne'
  | 'varselbrev'
  | 'brev'

export interface BehandlingRouteTypes {
  path: string
  description: string
  kreverBehandlingsstatus?: (behandling: IBehandlingReducer) => IBehandlingStatus
  sakstype?: SakType
}

const behandlingRoutes = (
  behandling: IBehandlingReducer
): Array<{ path: behandlingRouteTypes; element: React.JSX.Element }> => [
  { path: 'soeknadsoversikt', element: <Soeknadsoversikt behandling={behandling} /> },
  { path: 'revurderingsoversikt', element: <Revurderingsoversikt behandling={behandling} /> },
  { path: 'opphoeroversikt', element: <ManueltOpphoerOversikt behandling={behandling} /> },
  { path: 'vilkaarsvurdering', element: <Vilkaarsvurdering behandling={behandling} /> },
  { path: 'aktivitetsplikt', element: <Aktivitetsplikt behandling={behandling} /> },
  { path: 'trygdetid', element: <TrygdetidVisning behandling={behandling} /> },
  { path: 'beregningsgrunnlag', element: <Beregningsgrunnlag behandling={behandling} /> },
  { path: 'beregne', element: <Beregne behandling={behandling} /> },
  { path: 'varselbrev', element: <Varselbrev behandling={behandling} /> },
  { path: 'brev', element: <Vedtaksbrev behandling={behandling} /> },
]

const routeTypes = {
  soeknadsoversikt: {
    path: 'soeknadsoversikt',
    description: 'Søknadsoversikt',
  },
  revurderingsoversikt: {
    path: 'revurderingsoversikt',
    description: 'Revurderingsoversikt',
  },
  opphoeroversikt: {
    path: 'opphoeroversikt',
    description: 'Opphøroversikt',
  },
  vilkaarsvurdering: {
    path: 'vilkaarsvurdering',
    description: 'Vilkårsvurdering',
    kreverBehandlingsstatus: (behandling: IDetaljertBehandling) =>
      soeknadsoversiktErFerdigUtfylt(behandling) ? IBehandlingStatus.OPPRETTET : IBehandlingStatus.VILKAARSVURDERT,
  },
  aktivitetsplikt: {
    path: 'aktivitetsplikt',
    description: 'Oppfølging av aktivitet',
    kreverBehandlingsstatus: () => IBehandlingStatus.VILKAARSVURDERT,
    sakstype: SakType.OMSTILLINGSSTOENAD,
  },
  trygdetid: {
    path: 'trygdetid',
    description: 'Trygdetid',
    kreverBehandlingsstatus: () => IBehandlingStatus.TRYGDETID_OPPDATERT,
  },
  beregningsgrunnlag: {
    path: 'beregningsgrunnlag',
    description: 'Beregningsgrunnlag',
    kreverBehandlingsstatus: () => IBehandlingStatus.TRYGDETID_OPPDATERT,
  },
  beregning: {
    path: 'beregne',
    description: 'Beregning',
    kreverBehandlingsstatus: () => IBehandlingStatus.BEREGNET,
  },
  varselbrev: {
    path: 'varselbrev',
    description: 'Varselbrev',
    kreverBehandlingsstatus: () => IBehandlingStatus.BEREGNET,
  },
  brevBp: {
    path: 'brev',
    description: 'Vedtaksbrev',
    kreverBehandlingsstatus: () => IBehandlingStatus.BEREGNET,
  },
  brevOms: {
    path: 'brev',
    description: 'Vedtaksbrev',
    kreverBehandlingsstatus: () => IBehandlingStatus.AVKORTET,
  },
}

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

  const varselbrevAktivert = useFeatureEnabledMedDefault(FEATURE_TOGGLE_LAG_VARSELBREV, false)
  const aktuelleRoutes = hentAktuelleRoutes(behandling, varselbrevAktivert)

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

const hentAktuelleRoutes = (behandling: IBehandlingReducer | null, varselbrevAktivert: boolean) => {
  if (!behandling) return []

  const lagVarselbrev = varselbrevAktivert && behandling?.kilde === Vedtaksloesning.GJENOPPRETTA

  switch (behandling.behandlingType) {
    case IBehandlingsType.MANUELT_OPPHOER:
      return behandlingRoutes(behandling).filter((route) =>
        manueltOpphoerRoutes.map((pathinfo) => pathinfo.path).includes(route.path)
      )
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return behandlingRoutes(behandling).filter((route) =>
        soeknadRoutes(behandling, lagVarselbrev)
          .map((pathinfo) => pathinfo.path)
          .includes(route.path)
      )
    case IBehandlingsType.REVURDERING:
      return behandlingRoutes(behandling).filter((route) =>
        revurderingRoutes(behandling, lagVarselbrev)
          .map((pathinfo) => pathinfo.path)
          .includes(route.path)
      )
  }
}

export function soeknadRoutes(behandling: IBehandlingReducer, lagVarselbrev: boolean): Array<BehandlingRouteTypes> {
  const avslag = behandling.vilkårsprøving?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  const defaultRoutes: Array<BehandlingRouteTypes> = avslag
    ? [routeTypes.soeknadsoversikt, routeTypes.vilkaarsvurdering]
    : [
        routeTypes.soeknadsoversikt,
        routeTypes.vilkaarsvurdering,
        routeTypes.aktivitetsplikt,
        routeTypes.trygdetid,
        routeTypes.beregningsgrunnlag,
        routeTypes.beregning,
      ]

  if (lagVarselbrev) {
    defaultRoutes.push(routeTypes.varselbrev)
  }

  return leggTilBrevHvisKrevesAvBehandling(defaultRoutes, behandling).filter(
    routesAktuelleForSakstype(behandling.sakType)
  )
}

export function revurderingRoutes(behandling: IBehandlingReducer, lagVarselbrev: boolean): Array<BehandlingRouteTypes> {
  const opphoer =
    erOpphoer(behandling.revurderingsaarsak!!) ||
    behandling.vilkårsprøving?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  const defaultRoutes: Array<BehandlingRouteTypes> = opphoer
    ? [routeTypes.revurderingsoversikt, routeTypes.vilkaarsvurdering, routeTypes.beregning]
    : [
        routeTypes.revurderingsoversikt,
        routeTypes.vilkaarsvurdering,
        routeTypes.trygdetid,
        routeTypes.beregningsgrunnlag,
        routeTypes.beregning,
      ]

  if (lagVarselbrev) {
    defaultRoutes.push(routeTypes.varselbrev)
  }

  return leggTilBrevHvisKrevesAvBehandling(defaultRoutes, behandling).filter(
    routesAktuelleForSakstype(behandling.sakType)
  )
}

const leggTilBrevHvisKrevesAvBehandling = (
  routes: Array<BehandlingRouteTypes>,
  behandling: IBehandlingReducer
): Array<BehandlingRouteTypes> => {
  if (behandlingSkalSendeBrev(behandling.behandlingType, behandling.revurderingsaarsak)) {
    return [...routes, behandling.sakType == SakType.OMSTILLINGSSTOENAD ? routeTypes.brevOms : routeTypes.brevBp]
  }
  return routes
}

const routesAktuelleForSakstype = (sakType: SakType) => (route: BehandlingRouteTypes) =>
  route.sakstype === undefined || route.sakstype === sakType

export const manueltOpphoerRoutes: Array<BehandlingRouteTypes> = [routeTypes.opphoeroversikt, routeTypes.beregning]
