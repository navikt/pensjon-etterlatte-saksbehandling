import { useContext, useState } from 'react'
import { Alert, BodyShort, Box, Button, Label, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterRevurderingInfo,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { slettTotalVurdering, VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ForlengetOmsAktivitet, ForlengetOmsVurdering, lagreForlengetOmsVurdering } from '~shared/api/forlengetOms'
import { ForlengetOmsInfo } from '~shared/types/RevurderingInfo'

type Props = {
  behandling: IBehandlingReducer
  redigerbar: boolean
  vurdering?: ForlengetOmsVurdering
  aktivitet?: ForlengetOmsAktivitet
  onLagrerChange?: (lagrer: boolean) => void
  onLagret?: (vurdering: ForlengetOmsVurdering) => void
  resultatBegrunnelse?: { tekst: string; onChange: (tekst: string) => void }
  validerAktivitet?: () => boolean
  validerPeriode?: (onSuccess: (periode: ForlengetOmsInfo) => void) => void
  innvilgelseFeil?: string
}

export const ResultatForlengetOms = ({
  behandling,
  redigerbar,
  vurdering,
  aktivitet,
  onLagrerChange,
  onLagret,
  resultatBegrunnelse,
  validerAktivitet,
  validerPeriode,
  innvilgelseFeil,
}: Props) => {
  const dispatch = useAppDispatch()
  const { next } = useContext(BehandlingRouteContext)
  const [svar, setSvar] = useState<VilkaarsvurderingResultat | undefined>(
    vurdering
      ? vurdering.utfall === 'INNVILGELSE'
        ? VilkaarsvurderingResultat.OPPFYLT
        : VilkaarsvurderingResultat.IKKE_OPPFYLT
      : undefined
  )
  const [utfallFeil, setUtfallFeil] = useState<string>()
  const [lagreUtfallStatus, lagreUtfall] = useApiCall(lagreForlengetOmsVurdering)
  const [slettUtfallStatus, slettUtfall] = useApiCall(slettTotalVurdering)
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const aktivitetErValgt = resultatBegrunnelse !== undefined && validerAktivitet !== undefined
  const alleVilkaarErVurdert = !!vilkaarsvurdering && vilkaarsvurdering.vilkaar.every((vilkaar) => !!vilkaar.vurdering)
  const lagrer = isPending(lagreUtfallStatus) || isPending(slettUtfallStatus)
  const lagretVurdering = isSuccess(lagreUtfallStatus) ? lagreUtfallStatus.data.vurdering : vurdering
  const lagretUtfallErUtdatert =
    !!vilkaarsvurdering?.resultat &&
    (vilkaarsvurdering.virkningstidspunkt !== behandling.virkningstidspunkt?.dato ||
      !!vilkaarsvurdering.isGrunnlagUtdatert)

  const lagreResultat = (utfall: VilkaarsvurderingResultat, begrunnelse: string, periode?: ForlengetOmsInfo) => {
    if (!aktivitet) {
      setUtfallFeil('Svar på spørsmålene om aktivitet før vurderingen lagres.')
      return
    }
    onLagrerChange?.(true)
    lagreUtfall(
      {
        behandlingId: behandling.id,
        aktivitet,
        begrunnelse,
        utfall: utfall === VilkaarsvurderingResultat.OPPFYLT ? 'INNVILGELSE' : 'AVSLAG',
        periode,
      },
      (lagret) => {
        onLagrerChange?.(false)
        onLagret?.(lagret.vurdering)
        dispatch(updateVilkaarsvurdering(lagret.vilkaarsvurdering))
        dispatch(oppdaterRevurderingInfo({ revurderingInfo: lagret.periode ?? undefined, begrunnelse: undefined }))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      },
      () => onLagrerChange?.(false)
    )
  }

  const lagreVurdering = () => {
    if (!redigerbar || vilkaarsvurdering?.resultat || lagrer || !alleVilkaarErVurdert) {
      return
    }
    if (!resultatBegrunnelse || !validerAktivitet) {
      setUtfallFeil('Velg om bruker tar utdanning eller deltar i arbeidsrettet tiltak.')
      return
    }
    if (!validerAktivitet()) {
      return
    }
    const utfall = svar
    if (!utfall) {
      setUtfallFeil('Du må velge om forlenget OMS skal innvilges eller avslås.')
      return
    }
    if (utfall === VilkaarsvurderingResultat.OPPFYLT && innvilgelseFeil) {
      setUtfallFeil(innvilgelseFeil)
      return
    }
    setUtfallFeil(undefined)
    const kommentar = resultatBegrunnelse.tekst
    if (utfall === VilkaarsvurderingResultat.OPPFYLT) {
      if (!validerPeriode) {
        setUtfallFeil('Innvilgelse krever en gyldig fra og med- og til og med-måned.')
        return
      }
      validerPeriode((periode) => lagreResultat(utfall, kommentar, periode))
    } else {
      lagreResultat(utfall, kommentar)
    }
  }

  const gaaVidere = () => {
    if (!lagretVurdering || !vilkaarsvurdering?.resultat || lagrer || !alleVilkaarErVurdert || lagretUtfallErUtdatert) {
      return
    }
    if (!validerAktivitet) {
      setUtfallFeil('Velg om bruker tar utdanning eller deltar i arbeidsrettet tiltak.')
      return
    }
    if (validerAktivitet()) {
      next()
    }
  }

  const redigerUtfall = () => {
    slettUtfall(behandling.id, (vurdering) => {
      resultatBegrunnelse?.onChange(vilkaarsvurdering?.resultat?.kommentar ?? '')
      setSvar(vilkaarsvurdering?.resultat?.utfall)
      setUtfallFeil(undefined)
      dispatch(updateVilkaarsvurdering(vurdering))
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
    })
  }

  return (
    <VStack paddingInline="space-64 space-32" gap="space-16">
      <Box background="meta-purple-soft" borderRadius="12" padding="space-16">
        <VStack gap="space-16">
          <Label as="p">Utfall for forlenget omstillingsstønad</Label>
          {vilkaarsvurdering?.resultat ? (
            <>
              <BodyShort>
                {vilkaarsvurdering.resultat.utfall === VilkaarsvurderingResultat.OPPFYLT
                  ? 'Forlenget OMS innvilges'
                  : 'Forlenget OMS avslås'}
              </BodyShort>
              {redigerbar && (
                <Button
                  variant="tertiary"
                  size="small"
                  onClick={redigerUtfall}
                  loading={isPending(slettUtfallStatus)}
                  disabled={isPending(lagreUtfallStatus)}
                >
                  Rediger vurdering
                </Button>
              )}
              {utfallFeil && <ApiErrorAlert>{utfallFeil}</ApiErrorAlert>}
            </>
          ) : (
            <RadioGroup
              legend="Innvilg eller avslå forlenget OMS"
              hideLegend
              value={svar ?? ''}
              onChange={(utfall: VilkaarsvurderingResultat) => {
                setSvar(utfall)
                setUtfallFeil(undefined)
              }}
              disabled={!redigerbar || lagrer || !alleVilkaarErVurdert || !aktivitetErValgt}
              error={utfallFeil}
            >
              <Radio value={VilkaarsvurderingResultat.OPPFYLT}>Innvilg forlenget OMS</Radio>
              <Radio value={VilkaarsvurderingResultat.IKKE_OPPFYLT}>Avslå forlenget OMS</Radio>
            </RadioGroup>
          )}
          {!alleVilkaarErVurdert && <BodyShort>Alle vilkår må vurderes før man kan gå videre.</BodyShort>}
          {redigerbar && !vilkaarsvurdering?.resultat && (
            <Box>
              <Button
                size="small"
                onClick={lagreVurdering}
                loading={lagrer}
                disabled={!alleVilkaarErVurdert || !aktivitetErValgt}
              >
                Lagre
              </Button>
            </Box>
          )}
        </VStack>
      </Box>
      {lagretUtfallErUtdatert && (
        <Alert variant="warning">
          Virkningstidspunktet eller grunnlaget er endret. Velg «Rediger vurdering» og vurder utfallet på nytt.
        </Alert>
      )}
      {mapFailure(lagreUtfallStatus, (error) => (
        <ApiErrorAlert>Kunne ikke lagre vurderingen av aktivitet: {error.detail}</ApiErrorAlert>
      ))}
      {mapFailure(slettUtfallStatus, (error) => (
        <ApiErrorAlert>Kunne ikke åpne utfallet for redigering: {error.detail}</ApiErrorAlert>
      ))}
      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button
            onClick={gaaVidere}
            disabled={
              !lagretVurdering ||
              !vilkaarsvurdering?.resultat ||
              !alleVilkaarErVurdert ||
              lagretUtfallErUtdatert ||
              lagrer ||
              !aktivitetErValgt
            }
          >
            Neste
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </VStack>
  )
}
