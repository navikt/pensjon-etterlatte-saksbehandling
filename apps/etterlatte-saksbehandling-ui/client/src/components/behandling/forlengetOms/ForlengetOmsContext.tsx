import { createContext, ReactNode, useContext, useEffect, useState } from 'react'
import { Loader } from '@navikt/ds-react'
import {
  ForlengetOmsAktivitet,
  ForlengetOmsUtfall,
  ForlengetOmsVurdering,
  hentForlengetOmsVurdering,
  lagreForlengetOmsVurdering,
  Tiltaksomfang,
} from '~shared/api/forlengetOms'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterRevurderingInfo,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { erBehandlingRedigerbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending, isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { slettTotalVurdering, VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { JaNei } from '~shared/types/ISvar'
import { ApiErrorAlert } from '~ErrorBoundary'

//Denne skal brukes til mellomlagring i en annen PR, derfor to forskjellige ForlengetOms-typer
interface ForlengetOmsUtkast {
  aktivitetType: ForlengetOmsAktivitet['type'] | ''
  skole: string
  studium: string
  studererMinst50Prosent: JaNei | ''
  tiltak: string
  omfang: Tiltaksomfang | ''
  aktivitetsplan: JaNei | ''
  begrunnelse: string
  tom: string
  utfall: ForlengetOmsUtfall | ''
}

const lagUtkast = (
  behandling: IBehandlingReducer,
  vurdering?: ForlengetOmsVurdering | null,
  aktivitetType: ForlengetOmsUtkast['aktivitetType'] = vurdering?.aktivitet.type ?? ''
): ForlengetOmsUtkast => {
  const aktivitet = vurdering?.aktivitet
  const periode = behandling.revurderinginfo?.revurderingInfo
  return {
    aktivitetType,
    skole: aktivitet?.type === 'UTDANNING' ? aktivitet.skole : '',
    studium: aktivitet?.type === 'UTDANNING' ? aktivitet.studium : '',
    studererMinst50Prosent: aktivitet?.type === 'UTDANNING' ? aktivitet.studererMinst50Prosent : '',
    tiltak: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.tiltak : '',
    omfang: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.omfang : '',
    aktivitetsplan: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.aktivitetsplan : '',
    begrunnelse: vurdering?.begrunnelse ?? behandling.vilkaarsvurdering?.resultat?.kommentar ?? '',
    tom: periode?.type === 'FORLENGET_OMS' ? periode.tom : '',
    utfall: vurdering?.utfall ?? '',
  }
}

const hentAktivitet = (utkast: ForlengetOmsUtkast): ForlengetOmsAktivitet | undefined => {
  switch (utkast.aktivitetType) {
    case 'UTDANNING':
      return utkast.studererMinst50Prosent
        ? {
            type: 'UTDANNING',
            skole: utkast.skole,
            studium: utkast.studium,
            studererMinst50Prosent: utkast.studererMinst50Prosent,
          }
        : undefined
    case 'ARBEIDSRETTET_TILTAK':
      return utkast.omfang && utkast.aktivitetsplan
        ? {
            type: 'ARBEIDSRETTET_TILTAK',
            tiltak: utkast.tiltak,
            omfang: utkast.omfang,
            aktivitetsplan: utkast.aktivitetsplan,
          }
        : undefined
    case 'NEI':
      return { type: 'NEI' }
    default:
      return undefined
  }
}

const erAktivitetKomplett = (aktivitet: ForlengetOmsAktivitet | undefined) => {
  switch (aktivitet?.type) {
    case 'UTDANNING':
      return !!aktivitet.skole.trim() && !!aktivitet.studium.trim()
    case 'ARBEIDSRETTET_TILTAK':
      return !!aktivitet.tiltak.trim()
    case 'NEI':
      return true
    default:
      return false
  }
}

const useForlengetOmsState = (behandling: IBehandlingReducer) => {
  const dispatch = useAppDispatch()
  const saksbehandler = useInnloggetSaksbehandler()
  const [vurderingStatus, setVurderingStatus] = useState<Result<ForlengetOmsVurdering | null>>({ status: 'initial' })
  const [utkast, setUtkast] = useState(() => lagUtkast(behandling))
  const [periodeFeil, setPeriodeFeil] = useState<string>()
  const [lagreStatus, lagre] = useApiCall(lagreForlengetOmsVurdering)
  const [redigerStatus, rediger] = useApiCall(slettTotalVurdering)

  useEffect(() => {
    let aktiv = true
    setVurderingStatus({ status: 'pending' })
    hentForlengetOmsVurdering(behandling.id).then((response) => {
      if (!aktiv) return
      if (response.ok) {
        setVurderingStatus({ status: 'success', data: response.data })
        setUtkast(lagUtkast(behandling, response.data))
      } else {
        setVurderingStatus({ status: 'error', error: response })
      }
    })
    return () => {
      aktiv = false
    }
  }, [behandling.id])

  const vurdering = isSuccess(vurderingStatus) ? vurderingStatus.data : undefined
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const redigerbar = erBehandlingRedigerbar(behandling.status, behandling.sakEnhetId, saksbehandler.skriveEnheter)
  const lagrer = isPending(lagreStatus)
  const redigerer = isPending(redigerStatus)
  const opptatt = lagrer || redigerer
  const sporsmaalRedigerbare = redigerbar && !opptatt && !vilkaarsvurdering?.resultat
  const alleVilkaarErVurdert = !!vilkaarsvurdering && vilkaarsvurdering.vilkaar.every((vilkaar) => !!vilkaar.vurdering)
  const lagretUtfallErUtdatert =
    !!vilkaarsvurdering?.resultat &&
    (vilkaarsvurdering.virkningstidspunkt !== behandling.virkningstidspunkt?.dato ||
      !!vilkaarsvurdering.isGrunnlagUtdatert)
  const innvilgelseFeil =
    utkast.aktivitetType === 'NEI'
      ? 'Forlenget OMS kan bare avslås når bruker verken tar utdanning eller deltar i tiltak.'
      : utkast.aktivitetType === 'UTDANNING' && utkast.studererMinst50Prosent === JaNei.NEI
        ? 'Personen må studere minst 50% for å få stønad.'
        : undefined
  const aktivitet = hentAktivitet(utkast)
  const fom = behandling.virkningstidspunkt?.dato.slice(0, 7)
  const kanLagre =
    sporsmaalRedigerbare &&
    alleVilkaarErVurdert &&
    erAktivitetKomplett(aktivitet) &&
    !!utkast.begrunnelse.trim() &&
    !!utkast.utfall &&
    !(utkast.utfall === 'INNVILGELSE' && !!innvilgelseFeil)

  const oppdaterFelt = <K extends keyof ForlengetOmsUtkast>(felt: K, verdi: ForlengetOmsUtkast[K]) => {
    setUtkast((forrige) => ({ ...forrige, [felt]: verdi }))
    if (felt === 'tom') setPeriodeFeil(undefined)
  }

  const velgAktivitet = (type: ForlengetOmsUtkast['aktivitetType']) => {
    setUtkast(lagUtkast(behandling, vurdering, type))
    setPeriodeFeil(undefined)
  }

  const lagreVurdering = () => {
    if (!kanLagre || !aktivitet || !utkast.utfall) return
    if (utkast.utfall === 'INNVILGELSE' && (!fom || !utkast.tom || utkast.tom < fom)) {
      setPeriodeFeil(
        !utkast.tom ? 'Må fylles ut' : 'Velg en til og med-måned som er lik eller etter fra og med-måneden.'
      )
      return
    }
    lagre(
      {
        behandlingId: behandling.id,
        aktivitet,
        begrunnelse: utkast.begrunnelse,
        utfall: utkast.utfall,
        periode: utkast.utfall === 'INNVILGELSE' && fom ? { type: 'FORLENGET_OMS', fom, tom: utkast.tom } : undefined,
      },
      (lagret) => {
        setVurderingStatus({ status: 'success', data: lagret.vurdering })
        dispatch(updateVilkaarsvurdering(lagret.vilkaarsvurdering))
        dispatch(
          oppdaterRevurderingInfo(lagret.periode ? { revurderingInfo: lagret.periode, begrunnelse: undefined } : null)
        )
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      }
    )
  }

  const redigerVurdering = () => {
    if (!redigerbar || opptatt || !vilkaarsvurdering?.resultat) return
    rediger(behandling.id, (oppdatert) => {
      dispatch(updateVilkaarsvurdering(oppdatert))
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
    })
  }

  return {
    vurderingStatus,
    utkast,
    oppdaterFelt,
    velgAktivitet,
    lagreVurdering,
    redigerVurdering,
    lagreStatus,
    redigerStatus,
    lagrer,
    redigerer,
    opptatt,
    redigerbar,
    sporsmaalRedigerbare,
    alleVilkaarErVurdert,
    lagretUtfallErUtdatert,
    innvilgelseFeil,
    periodeFeil,
    fom,
    periodeRedigerbar: sporsmaalRedigerbare && behandling.status === IBehandlingStatus.OPPRETTET,
    avslagErLagret: vilkaarsvurdering?.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT,
    vilkaarsvurdering,
    kanLagre,
    kanVelgeUtfall: sporsmaalRedigerbare && alleVilkaarErVurdert && !!utkast.aktivitetType,
    kanGaaVidere:
      !!vurdering &&
      !!vilkaarsvurdering?.resultat &&
      alleVilkaarErVurdert &&
      !lagretUtfallErUtdatert &&
      !opptatt &&
      !!utkast.aktivitetType,
  }
}

const ForlengetOmsContext = createContext<ReturnType<typeof useForlengetOmsState> | undefined>(undefined)

export const ForlengetOmsProvider = ({
  behandling,
  children,
}: {
  behandling: IBehandlingReducer
  children: ReactNode
}) => {
  const state = useForlengetOmsState(behandling)
  return (
    <ForlengetOmsContext.Provider value={state}>
      {isSuccess(state.vurderingStatus)
        ? children
        : mapResult(state.vurderingStatus, {
            initial: <Loader title="Henter vurdering av aktivitet" />,
            pending: <Loader title="Henter vurdering av aktivitet" />,
            error: (error) => <ApiErrorAlert>Kunne ikke hente vurdering av aktivitet: {error.detail}</ApiErrorAlert>,
          })}
    </ForlengetOmsContext.Provider>
  )
}

export const useForlengetOms = () => {
  const context = useContext(ForlengetOmsContext)
  if (!context) throw new Error('useForlengetOms må brukes innenfor ForlengetOmsProvider')
  return context
}
