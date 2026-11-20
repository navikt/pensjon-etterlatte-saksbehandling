import { useContext, useState } from 'react'
import { Alert, BodyShort, Box, Button, Heading, Radio, RadioGroup, VStack } from '@navikt/ds-react'
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
import { oppdaterTotalVurdering, slettTotalVurdering, VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'

type Props = {
  behandling: IBehandlingReducer
  redigerbar: boolean
  resultatBegrunnelse: { tekst: string; onChange: (tekst: string) => void }
  validerAktivitet: () => boolean
  lagrePeriode: (onSuccess: () => void) => void
  lagrerPeriode: boolean
  innvilgelseFeil?: string
}

export const ResultatForlengetOms = ({
  behandling,
  redigerbar,
  resultatBegrunnelse,
  validerAktivitet,
  lagrePeriode,
  lagrerPeriode,
  innvilgelseFeil,
}: Props) => {
  const dispatch = useAppDispatch()
  const { next } = useContext(BehandlingRouteContext)
  const [svar, setSvar] = useState<VilkaarsvurderingResultat>()
  const [utfallFeil, setUtfallFeil] = useState<string>()
  const [lagreUtfallStatus, lagreUtfall] = useApiCall(oppdaterTotalVurdering)
  const [slettUtfallStatus, slettUtfall] = useApiCall(slettTotalVurdering)
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const alleVilkaarErVurdert = !!vilkaarsvurdering && vilkaarsvurdering.vilkaar.every((vilkaar) => !!vilkaar.vurdering)
  const lagrer = lagrerPeriode || isPending(lagreUtfallStatus) || isPending(slettUtfallStatus)
  const lagretUtfallErUtdatert =
    !!vilkaarsvurdering?.resultat &&
    (vilkaarsvurdering.virkningstidspunkt !== behandling.virkningstidspunkt?.dato ||
      !!vilkaarsvurdering.isGrunnlagUtdatert)

  const lagreResultat = (utfall: VilkaarsvurderingResultat) => {
    if (vilkaarsvurdering?.resultat) {
      next()
      return
    }
    lagreUtfall(
      { behandlingId: behandling.id, resultat: utfall, kommentar: resultatBegrunnelse.tekst },
      (vurdering) => {
        dispatch(updateVilkaarsvurdering(vurdering))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
        next()
      }
    )
  }

  const gaaVidere = () => {
    if (lagrer || !alleVilkaarErVurdert || lagretUtfallErUtdatert) {
      return
    }
    if (!validerAktivitet()) {
      return
    }
    const utfall = vilkaarsvurdering?.resultat?.utfall ?? svar
    if (!utfall) {
      setUtfallFeil('Du må velge om forlenget OMS skal innvilges eller avslås.')
      return
    }
    if (utfall === VilkaarsvurderingResultat.OPPFYLT && innvilgelseFeil) {
      setUtfallFeil(innvilgelseFeil)
      return
    }
    setUtfallFeil(undefined)
    if (utfall === VilkaarsvurderingResultat.OPPFYLT) {
      lagrePeriode(() => lagreResultat(utfall))
    } else {
      lagreResultat(utfall)
    }
  }

  const redigerUtfall = () => {
    slettUtfall(behandling.id, (vurdering) => {
      resultatBegrunnelse.onChange(vilkaarsvurdering?.resultat?.kommentar ?? '')
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
          <Heading size="medium" level="2">
            Utfall for forlenget OMS
          </Heading>
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
                  disabled={lagrerPeriode || isPending(lagreUtfallStatus)}
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
              disabled={!redigerbar || lagrer || !alleVilkaarErVurdert}
              error={utfallFeil}
            >
              <Radio value={VilkaarsvurderingResultat.OPPFYLT}>Innvilg forlenget OMS</Radio>
              <Radio value={VilkaarsvurderingResultat.IKKE_OPPFYLT}>Avslå forlenget OMS</Radio>
            </RadioGroup>
          )}
          {!alleVilkaarErVurdert && <BodyShort>Alle vilkår må vurderes før man kan gå videre.</BodyShort>}
        </VStack>
      </Box>
      {lagretUtfallErUtdatert && (
        <Alert variant="warning">
          Virkningstidspunktet eller grunnlaget er endret. Velg «Rediger vurdering» og vurder utfallet på nytt.
        </Alert>
      )}
      {mapFailure(lagreUtfallStatus, (error) => (
        <ApiErrorAlert>Kunne ikke lagre utfallet: {error.detail}</ApiErrorAlert>
      ))}
      {mapFailure(slettUtfallStatus, (error) => (
        <ApiErrorAlert>Kunne ikke åpne utfallet for redigering: {error.detail}</ApiErrorAlert>
      ))}
      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button onClick={gaaVidere} loading={lagrer} disabled={!alleVilkaarErVurdert || lagretUtfallErUtdatert}>
            Neste
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </VStack>
  )
}
