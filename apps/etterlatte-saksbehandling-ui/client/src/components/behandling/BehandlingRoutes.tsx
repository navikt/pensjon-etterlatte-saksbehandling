import React, { createContext, useEffect, useState } from 'react'
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
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Revurderingsoversikt } from '~components/behandling/revurderingsoversikt/Revurderingsoversikt'
import { hentGyldigeNavigeringsStatuser, soeknadsoversiktErFerdigUtfylt } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { Aktivitetsplikt } from '~components/behandling/aktivitetsplikt/Aktivitetsplikt'
import { SakType } from '~shared/types/sak'
import TrygdetidVisning from '~components/behandling/trygdetid/TrygdetidVisning'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { Varselbrev } from '~components/behandling/brev/Varselbrev'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { Personopplysninger } from '~shared/types/grunnlag'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { EtteroppgjoerRevurderingOversikt } from '~components/etteroppgjoer/revurdering/EtteroppgjoerRevurderingOversikt'
import { DetaljertEtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { useAppSelector } from '~store/Store'
import { JaNei } from '~shared/types/ISvar'

type BehandlingRouteTypesPath =
  | 'soeknadsoversikt'
  | 'revurderingsoversikt'
  | 'etteroppgjoeroversikt'
  | 'vilkaarsvurdering'
  | 'aktivitetsplikt'
  | 'trygdetid'
  | 'beregningsgrunnlag'
  | 'beregne'
  | 'varselbrev'
  | 'brev'

export interface BehandlingRouteType {
  path: BehandlingRouteTypesPath
  element: (behandling: IBehandlingReducer) => React.JSX.Element
  description: string
  kreverBehandlingsstatus?: (
    behandling: IBehandlingReducer,
    etteroppgjoer?: DetaljertEtteroppgjoerForbehandling | null
  ) => IBehandlingStatus
  sakstype?: SakType
}

export const behandlingroutes: Record<string, BehandlingRouteType> = {
  soeknadsoversikt: {
    path: 'soeknadsoversikt',
    description: 'Søknadsoversikt',
    element: (behandling: IBehandlingReducer) => <Soeknadsoversikt behandling={behandling} />,
  },
  revurderingsoversikt: {
    path: 'revurderingsoversikt',
    description: 'Revurderingsoversikt',
    element: (behandling: IBehandlingReducer) => <Revurderingsoversikt behandling={behandling} />,
  },
  etteroppgjoeroversikt: {
    path: 'etteroppgjoeroversikt',
    description: 'Etteroppgjøroversikt',
    element: (behandling: IBehandlingReducer) => <EtteroppgjoerRevurderingOversikt behandling={behandling} />,
  },
  vilkaarsvurdering: {
    path: 'vilkaarsvurdering',
    description: 'Vilkårsvurdering',
    element: (behandling: IBehandlingReducer) => <Vilkaarsvurdering behandling={behandling} />,
    kreverBehandlingsstatus: (behandling: IDetaljertBehandling) =>
      soeknadsoversiktErFerdigUtfylt(behandling) ? IBehandlingStatus.OPPRETTET : IBehandlingStatus.VILKAARSVURDERT,
  },
  aktivitetsplikt: {
    path: 'aktivitetsplikt',
    description: 'Oppfølging av aktivitet',
    element: (behandling: IBehandlingReducer) => <Aktivitetsplikt behandling={behandling} />,
    kreverBehandlingsstatus: () => IBehandlingStatus.VILKAARSVURDERT,
    sakstype: SakType.OMSTILLINGSSTOENAD,
  },
  trygdetid: {
    path: 'trygdetid',
    description: 'Trygdetid',
    element: (behandling: IBehandlingReducer) => <TrygdetidVisning behandling={behandling} />,
    kreverBehandlingsstatus: () => IBehandlingStatus.VILKAARSVURDERT,
  },
  beregningsgrunnlag: {
    path: 'beregningsgrunnlag',
    description: 'Beregningsgrunnlag',
    element: (behandling: IBehandlingReducer) => <Beregningsgrunnlag behandling={behandling} />,
    kreverBehandlingsstatus: () => IBehandlingStatus.TRYGDETID_OPPDATERT,
  },
  beregning: {
    path: 'beregne',
    description: 'Beregning',
    element: (behandling: IBehandlingReducer) => <Beregne behandling={behandling} />,
    kreverBehandlingsstatus: (behandling, etteroppgjoer) => {
      if (behandling.revurderingsaarsak !== Revurderingaarsak.ETTEROPPGJOER) {
        return IBehandlingStatus.BEREGNET
      }
      if (!etteroppgjoer || etteroppgjoer.forbehandling.endringErTilUgunstForBruker === JaNei.JA) {
        return IBehandlingStatus.FATTET_VEDTAK
      } else {
        return IBehandlingStatus.BEREGNET
      }
    },
  },
  varselbrev: {
    path: 'varselbrev',
    description: 'Varselbrev',
    element: (behandling: IBehandlingReducer) => <Varselbrev behandling={behandling} />,
    kreverBehandlingsstatus: () => IBehandlingStatus.VILKAARSVURDERT,
  },
  brev: {
    path: 'brev',
    element: (behandling: IBehandlingReducer) => <Vedtaksbrev behandling={behandling} />,
    description: 'Vedtaksbrev',
    kreverBehandlingsstatus: (behandling: IBehandlingReducer, etteroppgjoer) => {
      if (behandling.revurderingsaarsak !== Revurderingaarsak.ETTEROPPGJOER) {
        return behandlingHarVarselbrev(behandling) ? IBehandlingStatus.VILKAARSVURDERT : IBehandlingStatus.AVKORTET
      }
      if (!etteroppgjoer || etteroppgjoer.forbehandling.endringErTilUgunstForBruker === JaNei.JA) {
        return IBehandlingStatus.FATTET_VEDTAK
      } else {
        return behandlingHarVarselbrev(behandling) ? IBehandlingStatus.VILKAARSVURDERT : IBehandlingStatus.AVKORTET
      }
    },
  },
}
// skal kun brukes av Behandling som laster behandling, å newe opp denne kan få utilsiktede konsekvenser andre steder
export const useBehandlingRoutes = (): DefaultBehandlingRouteContextType => {
  const { currentRoute, goto } = useRouteNavigation()
  const behandling = useBehandling()
  const etteroppgjoer = useAppSelector((state) => state.etteroppgjoerReducer.etteroppgjoer)
  const personopplysninger = usePersonopplysninger()

  const aktuelleRoutes = hentAktuelleRoutes(behandling, personopplysninger)

  const currentRouteErGyldig = () => {
    return routeErGyldig(currentRoute)
  }

  const routeErGyldig = (route: string | undefined): boolean => {
    const alleRoutes: BehandlingRouteType[] = Object.values(behandlingroutes)
    const valgtRoute = alleRoutes.filter((value) => value.path === route)
    if (valgtRoute.length) {
      const pathInfo = valgtRoute[0]

      return (
        !pathInfo.kreverBehandlingsstatus ||
        (!!behandling &&
          hentGyldigeNavigeringsStatuser(behandling.status).includes(
            pathInfo.kreverBehandlingsstatus(behandling, etteroppgjoer)
          ))
      )
    } else {
      return false
    }
  }

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

  return {
    next,
    back,
    lastPage,
    firstPage,
    behandlingRoutes: aktuelleRoutes,
    currentRoute,
    goto,
    routeErGyldig: routeErGyldig,
    currentRouteErGyldig: currentRouteErGyldig,
  }
}

type DefaultBehandlingRouteContextType = {
  next: () => void
  back: () => void
  lastPage: boolean
  firstPage: boolean
  behandlingRoutes: BehandlingRouteType[]
  currentRoute: string | undefined
  goto: (path: BehandlingRouteTypesPath) => void
  routeErGyldig: (route: string | undefined) => boolean
  currentRouteErGyldig: () => boolean
}

const DefaultBehandlingRoutecontext: DefaultBehandlingRouteContextType = {
  next: () => {},
  back: () => {},
  lastPage: false,
  firstPage: true,
  behandlingRoutes: new Array<BehandlingRouteType>(),
  currentRoute: undefined,
  goto: () => {},
  routeErGyldig: () => false,
  currentRouteErGyldig: () => false,
}

export const BehandlingRouteContext = createContext<DefaultBehandlingRouteContextType>(DefaultBehandlingRoutecontext)

function useRouteNavigation() {
  const match = useMatch('/behandling/:behandlingId/:section')
  const [currentRoute, setCurrentRoute] = useState<string | undefined>(match?.params?.section)
  const navigate = useNavigate()

  useEffect(() => {
    setCurrentRoute(match?.params?.section)
  }, [match])

  const goto = (path: BehandlingRouteTypesPath) => {
    setCurrentRoute(path)
    navigate(`/behandling/${match?.params?.behandlingId}/${path}`)
  }

  return { currentRoute, goto }
}

export function behandlingHarVarselbrev(behandling: IBehandlingReducer | null) {
  return (
    behandling?.kilde === Vedtaksloesning.GJENOPPRETTA ||
    behandling?.revurderingsaarsak === Revurderingaarsak.AKTIVITETSPLIKT
  )
}

const hentAktuelleRoutes = (behandling: IBehandlingReducer | null, personopplysninger: Personopplysninger | null) => {
  if (!behandling) return []

  const lagVarselbrev = behandlingHarVarselbrev(behandling)
  const ukjentAvdoed = personopplysninger?.avdoede.length === 0
  const boddEllerArbeidetUtlandet = behandling.boddEllerArbeidetUtlandet?.boddEllerArbeidetUtlandet ?? false

  switch (behandling.behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return foerstegangsbehandlingRoutes(behandling, lagVarselbrev, ukjentAvdoed, boddEllerArbeidetUtlandet)
    case IBehandlingsType.REVURDERING:
      return revurderingRoutes(behandling, lagVarselbrev, ukjentAvdoed, boddEllerArbeidetUtlandet)
  }
}

function foerstegangsbehandlingRoutes(
  behandling: IBehandlingReducer,
  lagVarselbrev: boolean,
  ukjentAvdoed: boolean,
  boddEllerArbeidetUtlandet: boolean
): Array<BehandlingRouteType> {
  const avslag = behandling.vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  const defaultRoutes: Array<BehandlingRouteType> = avslag
    ? [behandlingroutes.soeknadsoversikt, behandlingroutes.vilkaarsvurdering]
    : [
        behandlingroutes.soeknadsoversikt,
        behandlingroutes.vilkaarsvurdering,
        behandlingroutes.aktivitetsplikt,
        behandlingroutes.trygdetid,
        behandlingroutes.beregningsgrunnlag,
        behandlingroutes.beregning,
      ]

  if (avslag && boddEllerArbeidetUtlandet && !ukjentAvdoed) {
    defaultRoutes.push(behandlingroutes.trygdetid)
  }

  if (lagVarselbrev) {
    defaultRoutes.push(behandlingroutes.varselbrev)
  }

  return leggTilBrevHvisKrevesAvBehandling(defaultRoutes, behandling).filter(
    routesAktuelleForSakstype(behandling.sakType)
  )
}

function revurderingRoutes(
  behandling: IBehandlingReducer,
  lagVarselbrev: boolean,
  ukjentAvdoed: boolean,
  boddEllerArbeidetUtlandet: boolean
): Array<BehandlingRouteType> {
  if (behandling.revurderingsaarsak === Revurderingaarsak.ETTEROPPGJOER) {
    return [behandlingroutes.etteroppgjoeroversikt, behandlingroutes.beregning, behandlingroutes.brev]
  }

  const opphoer = behandling.vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT
  const avslag = opphoer && behandling.revurderingsaarsak === Revurderingaarsak.NY_SOEKNAD

  const routesOpphoer = [behandlingroutes.revurderingsoversikt, behandlingroutes.vilkaarsvurdering]
  if (avslag && boddEllerArbeidetUtlandet && !ukjentAvdoed) {
    routesOpphoer.push(behandlingroutes.trygdetid)
  }
  if (opphoer && !avslag) {
    routesOpphoer.push(behandlingroutes.beregning)
  }

  const defaultRoutes: Array<BehandlingRouteType> = opphoer
    ? routesOpphoer
    : [
        behandlingroutes.revurderingsoversikt,
        behandlingroutes.vilkaarsvurdering,
        behandlingroutes.aktivitetsplikt,
        behandlingroutes.trygdetid,
        behandlingroutes.beregningsgrunnlag,
        behandlingroutes.beregning,
      ]

  if (lagVarselbrev) {
    defaultRoutes.push(behandlingroutes.varselbrev)
  }

  return leggTilBrevHvisKrevesAvBehandling(defaultRoutes, behandling).filter(
    routesAktuelleForSakstype(behandling.sakType)
  )
}

const leggTilBrevHvisKrevesAvBehandling = (
  routes: Array<BehandlingRouteType>,
  behandling: IBehandlingReducer
): Array<BehandlingRouteType> => {
  if (behandling.sendeBrev) {
    return [...routes, behandlingroutes.brev]
  }
  return routes
}

const routesAktuelleForSakstype = (sakType: SakType) => (route: BehandlingRouteType) =>
  route.sakstype === undefined || route.sakstype === sakType
