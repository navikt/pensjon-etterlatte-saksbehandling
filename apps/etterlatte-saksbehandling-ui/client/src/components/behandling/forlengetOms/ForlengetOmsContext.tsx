import { createContext, ReactNode, useContext, useEffect, useState } from 'react'
import { Loader } from '@navikt/ds-react'
import {
  AktivitetOgUtfall,
  ForlengetOmsAktivitet,
  ForlengetOmsUtfall,
  hentAktivitetOgUtfall,
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

interface Periode {
  fom: string
  tom: string
}

interface AktivitetUtdanning {
  skole: string
  studium: string
  studererMinst50Prosent: JaNei | ''
}

interface ArbeidsrettetTiltak {
  tiltak: string
  omfang: Tiltaksomfang | ''
  aktivitetsplan: JaNei | ''
}

const hentAktivitet = (
  //TODO bort
  aktivitetType: ForlengetOmsAktivitet['type'] | '',
  aktivitetUtdanning: AktivitetUtdanning,
  arbeidsrettetTiltak: ArbeidsrettetTiltak
): ForlengetOmsAktivitet | undefined => {
  switch (aktivitetType) {
    case 'UTDANNING':
      return aktivitetUtdanning.studererMinst50Prosent
        ? {
            type: 'UTDANNING',
            skole: aktivitetUtdanning.skole,
            studium: aktivitetUtdanning.studium,
            studererMinst50Prosent: aktivitetUtdanning.studererMinst50Prosent,
          }
        : undefined
    case 'ARBEIDSRETTET_TILTAK':
      return arbeidsrettetTiltak.omfang && arbeidsrettetTiltak.aktivitetsplan
        ? {
            type: 'ARBEIDSRETTET_TILTAK',
            tiltak: arbeidsrettetTiltak.tiltak,
            omfang: arbeidsrettetTiltak.omfang,
            aktivitetsplan: arbeidsrettetTiltak.aktivitetsplan,
          }
        : undefined
    case 'NEI':
      return { type: 'NEI' }
    default:
      return undefined
  }
}

const erAktivitetKomplett = (aktivitet: ForlengetOmsAktivitet | undefined) => {
  //TODO bort
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
  const [hentAktivitetOgUtfallStatus, settHentAktivitetOgUtfallStatus] = useState<Result<AktivitetOgUtfall | null>>({
    status: 'initial',
  })
  const [aktivitetType, settAktivitetType] = useState<ForlengetOmsAktivitet['type'] | ''>('')
  const [utfall, settUtfall] = useState<ForlengetOmsUtfall | ''>('')
  const [begrunnelse, settBegrunnelse] = useState('')
  const [periode, settPeriode] = useState<Periode>({
    fom: '',
    tom: '',
  })
  const [periodeFeil, setPeriodeFeil] = useState<string>()
  const [lagreStatus, lagre] = useApiCall(lagreForlengetOmsVurdering)
  const [redigerStatus, rediger] = useApiCall(slettTotalVurdering)
  const [aktivitetUtdanning, settAktivitetUtdanning] = useState<AktivitetUtdanning>({
    skole: '',
    studium: '',
    studererMinst50Prosent: '',
  })
  const [arbeidsrettetTiltak, settArbeidsrettetTiltak] = useState<ArbeidsrettetTiltak>({
    tiltak: '',
    omfang: '',
    aktivitetsplan: '',
  })

  useEffect(() => {
    settHentAktivitetOgUtfallStatus({ status: 'pending' })
    hentAktivitetOgUtfall(behandling.id).then((response) => {
      if (response.ok) {
        const aktivitet = response.data?.aktivitet
        settHentAktivitetOgUtfallStatus({ status: 'success', data: response.data })
        settAktivitetType(aktivitet?.type ?? '')
        const lagretPeriode = behandling.revurderinginfo?.revurderingInfo
        settPeriode({
          fom: behandling.virkningstidspunkt?.dato.slice(0, 7) ?? '',
          tom: lagretPeriode?.type === 'FORLENGET_OMS' ? lagretPeriode.tom : '',
        })
        settUtfall(response.data?.utfall ?? '')
        settBegrunnelse(response.data?.begrunnelse ?? behandling.vilkaarsvurdering?.resultat?.kommentar ?? '')
        settAktivitetUtdanning({
          skole: aktivitet?.type === 'UTDANNING' ? aktivitet.skole : '',
          studium: aktivitet?.type === 'UTDANNING' ? aktivitet.studium : '',
          studererMinst50Prosent: aktivitet?.type === 'UTDANNING' ? aktivitet.studererMinst50Prosent : '',
        })
        settArbeidsrettetTiltak({
          tiltak: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.tiltak : '',
          omfang: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.omfang : '',
          aktivitetsplan: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.aktivitetsplan : '',
        })
      } else {
        settHentAktivitetOgUtfallStatus({ status: 'error', error: response })
      }
    })
  }, [behandling.id])

  useEffect(() => {
    settPeriode((forrige) => ({ ...forrige, fom: behandling.virkningstidspunkt?.dato.slice(0, 7) ?? '' }))
  }, [behandling.virkningstidspunkt?.dato])

  useEffect(() => {
    setPeriodeFeil(undefined)
  }, [periode.fom, periode.tom])

  const lagretAktivitetOgUtfall = isSuccess(hentAktivitetOgUtfallStatus) ? hentAktivitetOgUtfallStatus.data : undefined
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
    aktivitetType === 'NEI'
      ? 'Forlenget OMS kan bare avslås når bruker verken tar utdanning eller deltar i tiltak.'
      : aktivitetType === 'UTDANNING' && aktivitetUtdanning.studererMinst50Prosent === JaNei.NEI
        ? 'Personen må studere minst 50% for å få stønad.'
        : undefined
  const aktivitet = hentAktivitet(aktivitetType, aktivitetUtdanning, arbeidsrettetTiltak)
  const kanLagre =
    sporsmaalRedigerbare &&
    alleVilkaarErVurdert &&
    erAktivitetKomplett(aktivitet) &&
    !!begrunnelse.trim() &&
    !!utfall &&
    !(utfall === 'INNVILGELSE' && !!innvilgelseFeil)

  const oppdaterUtdanning = (endringer: Partial<AktivitetUtdanning>) => {
    settAktivitetUtdanning((forrige) => ({ ...forrige, ...endringer }))
  }

  const oppdaterArbeidsrettetTiltak = (endringer: Partial<ArbeidsrettetTiltak>) => {
    settArbeidsrettetTiltak((forrige) => ({ ...forrige, ...endringer }))
  }

  const velgAktivitet = (type: ForlengetOmsAktivitet['type'] | '') => {
    const aktivitet = lagretAktivitetOgUtfall?.aktivitet
    settAktivitetType(type)
    const lagretPeriode = behandling.revurderinginfo?.revurderingInfo
    settPeriode({
      fom: behandling.virkningstidspunkt?.dato.slice(0, 7) ?? '',
      tom: lagretPeriode?.type === 'FORLENGET_OMS' ? lagretPeriode.tom : '',
    })
    settUtfall(lagretAktivitetOgUtfall?.utfall ?? '')
    settAktivitetUtdanning({
      skole: aktivitet?.type === 'UTDANNING' ? aktivitet.skole : '',
      studium: aktivitet?.type === 'UTDANNING' ? aktivitet.studium : '',
      studererMinst50Prosent: aktivitet?.type === 'UTDANNING' ? aktivitet.studererMinst50Prosent : '',
    })
    settArbeidsrettetTiltak({
      tiltak: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.tiltak : '',
      omfang: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.omfang : '',
      aktivitetsplan: aktivitet?.type === 'ARBEIDSRETTET_TILTAK' ? aktivitet.aktivitetsplan : '',
    })
    setPeriodeFeil(undefined)
  }

  const lagreVurdering = () => {
    if (!kanLagre || !aktivitet || !utfall) return
    if (utfall === 'INNVILGELSE' && (!periode.fom || !periode.tom || periode.tom < periode.fom)) {
      setPeriodeFeil(
        !periode.tom ? 'Må fylles ut' : 'Velg en til og med-måned som er lik eller etter fra og med-måneden.'
      )
      return
    }
    lagre(
      {
        behandlingId: behandling.id,
        aktivitet,
        begrunnelse,
        utfall,
        periode: utfall === 'INNVILGELSE' ? { type: 'FORLENGET_OMS', ...periode } : undefined,
      },
      (lagret) => {
        settHentAktivitetOgUtfallStatus({ status: 'success', data: lagret.vurdering })
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
    hentAktivitetOgUtfallStatus,
    aktivitetType,
    utfall,
    settUtfall,
    begrunnelse,
    settBegrunnelse,
    periode,
    settPeriode,
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
    periodeRedigerbar: sporsmaalRedigerbare && behandling.status === IBehandlingStatus.OPPRETTET,
    avslagErLagret: vilkaarsvurdering?.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT,
    vilkaarsvurdering,
    kanLagre,
    kanVelgeUtfall: sporsmaalRedigerbare && alleVilkaarErVurdert && !!aktivitetType,
    kanGaaVidere:
      !!lagretAktivitetOgUtfall &&
      !!vilkaarsvurdering?.resultat &&
      alleVilkaarErVurdert &&
      !lagretUtfallErUtdatert &&
      !opptatt &&
      !!aktivitetType,
    aktivitetUtdanning,
    oppdaterUtdanning,
    arbeidsrettetTiltak,
    oppdaterArbeidsrettetTiltak,
  }
}

const ForlengetOmsContext = createContext<ReturnType<typeof useForlengetOmsState> | undefined>(undefined) //TODO rekkefølge/bort

export const ForlengetOmsProvider = ({
  //TODO rekkefølge/bort
  behandling,
  children,
}: {
  behandling: IBehandlingReducer
  children: ReactNode
}) => {
  const state = useForlengetOmsState(behandling)
  return (
    <ForlengetOmsContext.Provider value={state}>
      {isSuccess(state.hentAktivitetOgUtfallStatus)
        ? children
        : mapResult(state.hentAktivitetOgUtfallStatus, {
            initial: <Loader title="Henter aktivitet og utfall" />,
            pending: <Loader title="Henter aktivitet og utfall" />,
            error: (error) => <ApiErrorAlert>Kunne ikke hente aktivitet og utfall: {error.detail}</ApiErrorAlert>,
          })}
    </ForlengetOmsContext.Provider>
  )
}

export const useForlengetOms = () => {
  //TODO rekkefølge/bort
  const context = useContext(ForlengetOmsContext)
  if (!context) throw new Error('useForlengetOms må brukes innenfor ForlengetOmsProvider')
  return context
}
