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
  validerPeriode,
  innvilgelseFeil,
}: Props) => {
  const dispatch = useAppDispatch()
  const { next } = useContext(BehandlingRouteContext)
  const [svar, setSvar] = useState<ForlengetOmsUtfall | undefined>(vurdering?.utfall)
  const [slettUtfallStatus, slettUtfall] = useApiCall(slettTotalVurdering)
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const aktivitetErValgt = begrunnelse !== undefined
  const alleVilkaarErVurdert = !!vilkaarsvurdering && vilkaarsvurdering.vilkaar.every((vilkaar) => !!vilkaar.vurdering)
  const redigerer = isPending(slettUtfallStatus)
  const lagretUtfallErUtdatert =
    !!vilkaarsvurdering?.resultat &&
    (vilkaarsvurdering.virkningstidspunkt !== behandling.virkningstidspunkt?.dato ||
      !!vilkaarsvurdering.isGrunnlagUtdatert)

  const validerAktivitet = () => {
    switch (aktivitet?.type) {
      case 'UTDANNING':
        return (
          !!aktivitet.skole.trim() &&
          !!aktivitet.studium.trim() &&
          !!aktivitet.studererMinst50Prosent &&
          !!begrunnelse?.trim()
        )
      case 'ARBEIDSRETTET_TILTAK':
        return !!aktivitet.tiltak.trim() && !!aktivitet.omfang && !!aktivitet.aktivitetsplan && !!begrunnelse?.trim()
      case 'NEI':
        return !!begrunnelse?.trim()
      default:
        return false
    }
  }

  const lagreVurdering = () => {
    if (!aktivitet || begrunnelse === undefined || !svar) {
      return
    }

    const request = { aktivitet, begrunnelse, utfall: svar }
    if (svar === 'INNVILGELSE') {
      validerPeriode?.((periode) => onLagre({ ...request, periode }))
    } else {
      onLagre(request)
    }
  }

  const redigerUtfall = () => {
    slettUtfall(behandling.id, (vurdering) => {
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
            </>
          ) : (
            <RadioGroup
              legend="Innvilg eller avslå forlenget OMS"
              hideLegend
              value={svar ?? ''}
              onChange={setSvar}
              disabled={!redigerbar || lagrer || redigerer || !alleVilkaarErVurdert || !aktivitetErValgt}
            >
              <Radio value="INNVILGELSE">Innvilg forlenget OMS</Radio>
              <Radio value="AVSLAG">Avslå forlenget OMS</Radio>
            </RadioGroup>
          )}
          {!vilkaarsvurdering?.resultat && innvilgelseFeil && <BodyShort>{innvilgelseFeil}</BodyShort>}
          {!alleVilkaarErVurdert && <BodyShort>Alle vilkår må vurderes før man kan gå videre.</BodyShort>}
          {redigerbar && !vilkaarsvurdering?.resultat && (
            <Box>
              <Button
                size="small"
                onClick={lagreVurdering}
                loading={lagrer || redigerer}
                disabled={
                  !alleVilkaarErVurdert ||
                  !validerAktivitet() ||
                  !svar ||
                  lagrer ||
                  redigerer ||
                  (svar === 'INNVILGELSE' && !!innvilgelseFeil)
                }
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
