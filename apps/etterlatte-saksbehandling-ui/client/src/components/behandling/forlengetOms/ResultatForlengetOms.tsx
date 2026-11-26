import { useContext, useState } from 'react'
import { Alert, BodyShort, Box, Button, Label, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { slettTotalVurdering, VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import {
  ForlengetOmsAktivitet,
  ForlengetOmsUtfall,
  ForlengetOmsVurdering,
  LagreForlengetOmsVurdering,
} from '~shared/api/forlengetOms'
import { ForlengetOmsInfo } from '~shared/types/RevurderingInfo'

type Props = {
  behandling: IBehandlingReducer
  redigerbar: boolean
  vurdering?: ForlengetOmsVurdering
  aktivitet?: ForlengetOmsAktivitet
  lagrer: boolean
  onLagre: (request: LagreForlengetOmsVurdering) => void
  begrunnelse?: string
  validerAktivitet?: () => boolean
  validerPeriode?: (onSuccess: (periode: ForlengetOmsInfo) => void) => void
  innvilgelseFeil?: string
}

export const ResultatForlengetOms = ({
  behandling,
  redigerbar,
  vurdering,
  aktivitet,
  lagrer,
  onLagre,
  begrunnelse,
  validerAktivitet,
  validerPeriode,
  innvilgelseFeil,
}: Props) => {
  const dispatch = useAppDispatch()
  const { next } = useContext(BehandlingRouteContext)
  const [svar, setSvar] = useState<ForlengetOmsUtfall | undefined>(vurdering?.utfall)
  const [utfallFeil, setUtfallFeil] = useState<string>()
  const [slettUtfallStatus, slettUtfall] = useApiCall(slettTotalVurdering)
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const aktivitetErValgt = begrunnelse !== undefined && validerAktivitet !== undefined
  const alleVilkaarErVurdert = !!vilkaarsvurdering && vilkaarsvurdering.vilkaar.every((vilkaar) => !!vilkaar.vurdering)
  const redigerer = isPending(slettUtfallStatus)
  const lagretUtfallErUtdatert =
    !!vilkaarsvurdering?.resultat &&
    (vilkaarsvurdering.virkningstidspunkt !== behandling.virkningstidspunkt?.dato ||
      !!vilkaarsvurdering.isGrunnlagUtdatert)

  const lagreResultat = (utfall: ForlengetOmsUtfall, begrunnelse: string, periode?: ForlengetOmsInfo) => {
    if (!aktivitet) {
      setUtfallFeil('Svar på spørsmålene om aktivitet før vurderingen lagres.')
      return
    }
    onLagre({ aktivitet, begrunnelse, utfall, periode })
  }

  const lagreVurdering = () => {
    if (!redigerbar || vilkaarsvurdering?.resultat || lagrer || redigerer || !alleVilkaarErVurdert) {
      return
    }
    if (begrunnelse === undefined || !validerAktivitet) {
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
    if (utfall === 'INNVILGELSE' && innvilgelseFeil) {
      setUtfallFeil(innvilgelseFeil)
      return
    }
    setUtfallFeil(undefined)
    if (utfall === 'INNVILGELSE') {
      if (!validerPeriode) {
        setUtfallFeil('Innvilgelse krever en gyldig fra og med- og til og med-måned.')
        return
      }
      validerPeriode((periode) => lagreResultat(utfall, begrunnelse, periode))
    } else {
      lagreResultat(utfall, begrunnelse)
    }
  }

  const redigerUtfall = () => {
    slettUtfall(behandling.id, (vurdering) => {
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
                <Button variant="tertiary" size="small" onClick={redigerUtfall} loading={redigerer} disabled={lagrer}>
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
              onChange={(utfall: ForlengetOmsUtfall) => {
                setSvar(utfall)
                setUtfallFeil(undefined)
              }}
              disabled={!redigerbar || lagrer || redigerer || !alleVilkaarErVurdert || !aktivitetErValgt}
              error={utfallFeil}
            >
              <Radio value="INNVILGELSE">Innvilg forlenget OMS</Radio>
              <Radio value="AVSLAG">Avslå forlenget OMS</Radio>
            </RadioGroup>
          )}
          {!alleVilkaarErVurdert && <BodyShort>Alle vilkår må vurderes før man kan gå videre.</BodyShort>}
          {redigerbar && !vilkaarsvurdering?.resultat && (
            <Box>
              <Button
                size="small"
                onClick={lagreVurdering}
                loading={lagrer || redigerer}
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
      {mapFailure(slettUtfallStatus, (error) => (
        <ApiErrorAlert>Kunne ikke åpne utfallet for redigering: {error.detail}</ApiErrorAlert>
      ))}
      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button
            onClick={next}
            disabled={
              !vurdering ||
              !vilkaarsvurdering?.resultat ||
              !alleVilkaarErVurdert ||
              lagretUtfallErUtdatert ||
              lagrer ||
              redigerer ||
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
