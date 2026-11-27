import { createContext, Dispatch, ReactNode, SetStateAction, useContext, useEffect, useState } from 'react'
import { Loader } from '@navikt/ds-react'
import {
  AktivitetOgUtfall,
  ForlengetOmsAktivitet,
  ForlengetOmsUtfall,
  hentAktivitetOgUtfall,
  LagretForlengetOmsVurdering,
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
import { IVilkaarsvurdering, slettTotalVurdering } from '~shared/api/vilkaarsvurdering'
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

interface ForlengetOmsContextValue {
  aktivitetType: ForlengetOmsAktivitet['type'] | ''
  utfall: ForlengetOmsUtfall | ''
  settUtfall: Dispatch<SetStateAction<ForlengetOmsUtfall | ''>>
  begrunnelse: string
  settBegrunnelse: Dispatch<SetStateAction<string>>
  periode: Periode
  settPeriode: Dispatch<SetStateAction<Periode>>
  settAktivitetType: Dispatch<SetStateAction<ForlengetOmsAktivitet['type'] | ''>>
  lagreVurdering: () => void
  redigerVurdering: () => void
  lagreStatus: Result<LagretForlengetOmsVurdering>
  redigerStatus: Result<IVilkaarsvurdering>
  harTilgang: boolean
  kanRedigere: boolean
  alleVilkaarErVurdert: boolean
  lagretUtfallErUtdatert: boolean
  vilkaarsvurdering: IVilkaarsvurdering | undefined
  kanLagre: boolean
  kanVelgeUtfall: boolean
  kanGaaVidere: boolean
  aktivitetUtdanning: AktivitetUtdanning
  oppdaterUtdanning: (endringer: Partial<AktivitetUtdanning>) => void
  arbeidsrettetTiltak: ArbeidsrettetTiltak
  oppdaterArbeidsrettetTiltak: (endringer: Partial<ArbeidsrettetTiltak>) => void
}

interface Props {
  behandling: IBehandlingReducer
  children: ReactNode
}

const ForlengetOmsContext = createContext<ForlengetOmsContextValue | undefined>(undefined)

const tilAktivitet = (
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

export const ForlengetOmsProvider = ({ behandling, children }: Props) => {
  const dispatch = useAppDispatch()
  const saksbehandler = useInnloggetSaksbehandler()
  const [hentAktivitetOgUtfallStatus, settHentAktivitetOgUtfallStatus] = useState<Result<AktivitetOgUtfall | null>>({
    status: 'initial',
  })
  const [aktivitetType, settAktivitetType] = useState<ForlengetOmsAktivitet['type'] | ''>('')
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
  const [periode, settPeriode] = useState<Periode>({
    fom: '',
    tom: '',
  })
  const [begrunnelse, settBegrunnelse] = useState('')
  const [utfall, settUtfall] = useState<ForlengetOmsUtfall | ''>('')
  const [lagreStatus, lagre] = useApiCall(lagreForlengetOmsVurdering)
  const [redigerStatus, rediger] = useApiCall(slettTotalVurdering)

  useEffect(() => {
    settHentAktivitetOgUtfallStatus({ status: 'pending' })
    hentAktivitetOgUtfall(behandling.id).then((response) => {
      if (response.ok) {
        const aktivitet = response.data?.aktivitet
        settHentAktivitetOgUtfallStatus({ status: 'success', data: response.data })

        settAktivitetType(aktivitet?.type ?? '')

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

        const lagretPeriode = behandling.revurderinginfo?.revurderingInfo
        settPeriode({
          fom: lagretPeriode?.type === 'FORLENGET_OMS' ? lagretPeriode.fom : '',
          tom: lagretPeriode?.type === 'FORLENGET_OMS' ? lagretPeriode.tom : '',
        })

        settBegrunnelse(response.data?.begrunnelse ?? behandling.vilkaarsvurdering?.resultat?.kommentar ?? '')
        settUtfall(response.data?.utfall ?? '')
      } else {
        settHentAktivitetOgUtfallStatus({ status: 'error', error: response })
      }
    })
  }, [behandling.id])

  const lagretAktivitetOgUtfall = isSuccess(hentAktivitetOgUtfallStatus) ? hentAktivitetOgUtfallStatus.data : undefined
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const harTilgang = erBehandlingRedigerbar(behandling.status, behandling.sakEnhetId, saksbehandler.skriveEnheter)
  const kanRedigere = harTilgang && !vilkaarsvurdering?.resultat && !isPending(lagreStatus) && !isPending(redigerStatus)
  const alleVilkaarErVurdert = !!vilkaarsvurdering && vilkaarsvurdering.vilkaar.every((vilkaar) => !!vilkaar.vurdering)
  const lagretUtfallErUtdatert =
    !!vilkaarsvurdering?.resultat &&
    (vilkaarsvurdering.virkningstidspunkt !== behandling.virkningstidspunkt?.dato ||
      !!vilkaarsvurdering.isGrunnlagUtdatert)
  const aktivitet = tilAktivitet(aktivitetType, aktivitetUtdanning, arbeidsrettetTiltak)
  const kanLagre = kanRedigere && alleVilkaarErVurdert && !!begrunnelse.trim() && !!utfall

  const oppdaterUtdanning = (endringer: Partial<AktivitetUtdanning>) => {
    settAktivitetUtdanning((forrige) => ({ ...forrige, ...endringer }))
  }

  const oppdaterArbeidsrettetTiltak = (endringer: Partial<ArbeidsrettetTiltak>) => {
    settArbeidsrettetTiltak((forrige) => ({ ...forrige, ...endringer }))
  }

  const lagreVurdering = () => {
    if (!kanLagre || !aktivitet || !utfall) return
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
    if (!harTilgang || isPending(redigerStatus) || isPending(lagreStatus) || !vilkaarsvurdering?.resultat) {
      return
    }
    rediger(behandling.id, (oppdatert) => {
      dispatch(updateVilkaarsvurdering(oppdatert))
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
    })
  }

  const value: ForlengetOmsContextValue = {
    aktivitetType,
    settAktivitetType,
    utfall,
    settUtfall,
    begrunnelse,
    settBegrunnelse,
    periode,
    settPeriode,
    lagreVurdering,
    redigerVurdering,
    lagreStatus,
    redigerStatus,
    harTilgang,
    kanRedigere,
    alleVilkaarErVurdert,
    lagretUtfallErUtdatert,
    vilkaarsvurdering,
    kanLagre,
    kanVelgeUtfall: kanRedigere && alleVilkaarErVurdert && !!aktivitetType,
    kanGaaVidere:
      !!lagretAktivitetOgUtfall &&
      !!vilkaarsvurdering?.resultat &&
      alleVilkaarErVurdert &&
      !lagretUtfallErUtdatert &&
      !isPending(lagreStatus) &&
      !isPending(redigerStatus) &&
      !!aktivitetType,
    aktivitetUtdanning,
    oppdaterUtdanning,
    arbeidsrettetTiltak,
    oppdaterArbeidsrettetTiltak,
  }
  return (
    <ForlengetOmsContext.Provider value={value}>
      {isSuccess(hentAktivitetOgUtfallStatus)
        ? children
        : mapResult(hentAktivitetOgUtfallStatus, {
            initial: <Loader title="Henter aktivitet og utfall" />,
            pending: <Loader title="Henter aktivitet og utfall" />,
            error: (error) => <ApiErrorAlert>Kunne ikke hente aktivitet og utfall: {error.detail}</ApiErrorAlert>,
          })}
    </ForlengetOmsContext.Provider>
  )
}

export const useForlengetOms = () => {
  const context = useContext(ForlengetOmsContext)
  if (!context) throw new Error('useForlengetOms må brukes innenfor ForlengetOmsProvider')
  return context
}
