import { useContext } from 'react'
import { BodyShort, Box, Button, Label, Radio, RadioGroup, Textarea, VStack } from '@navikt/ds-react'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { useBehandling } from '~components/behandling/useBehandling'
import { OppdatertGrunnlagAlert } from '~components/behandling/trygdetid/Grunnlagopplysninger'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useForlengetOms } from './ForlengetOmsContext'
import { ForlengetOmsUtfall } from '~shared/api/forlengetOms'

export const ResultatForlengetOms = () => {
  const {
    utfall,
    settUtfall,
    begrunnelse,
    settBegrunnelse,
    harTilgang,
    kanRedigere,
    lagreVurdering,
    redigerVurdering,
    redigerStatus,
    vilkaarsvurdering,
    kanLagre,
    kanVelgeUtfall,
    kanGaaVidere,
    lagreStatus,
  } = useForlengetOms()
  const { next } = useContext(BehandlingRouteContext)
  const behandling = useBehandling()
  const virkningstidspunktSamsvarer = behandling?.virkningstidspunkt?.dato === vilkaarsvurdering?.virkningstidspunkt

  return (
    <VStack paddingInline="space-64 space-32" gap="space-16">
      <VStack gap="space-16" maxWidth="30rem">
        <Textarea
          label="Begrunnelse"
          value={begrunnelse}
          onChange={(event) => settBegrunnelse(event.target.value)}
          minRows={6}
          readOnly={!kanRedigere}
        />
      </VStack>
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
              {harTilgang && (
                <Button
                  variant="tertiary"
                  size="small"
                  onClick={redigerVurdering}
                  loading={isPending(redigerStatus)}
                  disabled={isPending(lagreStatus)}
                >
                  Rediger vurdering
                </Button>
              )}
            </>
          ) : (
            <RadioGroup
              legend="Innvilg eller avslå forlenget OMS"
              hideLegend
              value={utfall}
              onChange={(verdi: ForlengetOmsUtfall) => settUtfall(verdi)}
              disabled={!kanVelgeUtfall}
            >
              <Radio value="INNVILGELSE">Innvilg forlenget OMS</Radio>
              <Radio value="AVSLAG">Avslå forlenget OMS</Radio>
            </RadioGroup>
          )}
          {harTilgang && !vilkaarsvurdering?.resultat && (
            <Box>
              <Button
                size="small"
                onClick={lagreVurdering}
                loading={isPending(lagreStatus) || isPending(redigerStatus)}
                disabled={!kanLagre}
              >
                Lagre
              </Button>
            </Box>
          )}
        </VStack>
      </Box>
      {mapFailure(lagreStatus, (error) => (
        <ApiErrorAlert>Kunne ikke lagre vurderingen av aktivitet: {error.detail}</ApiErrorAlert>
      ))}
      {vilkaarsvurdering?.resultat && !virkningstidspunktSamsvarer && (
        <OppdatertGrunnlagAlert variant="warning">
          Virkningstidspunktet er endret siden vilkårene ble vurdert sist. Du må se over vurderingene og sjekke at de
          fortsatt er riktige. Velg &quot;Rediger vurdering&quot; for å gjøre dette.
        </OppdatertGrunnlagAlert>
      )}
      {harTilgang && vilkaarsvurdering?.isGrunnlagUtdatert && (
        <OppdatertGrunnlagAlert variant="warning">
          OBS! Grunnlaget for vilkårsvurderingen har blitt oppdatert siden sist. <br />
          Du må se over vurderingene og sjekke at de fortsatt er riktige.
        </OppdatertGrunnlagAlert>
      )}
      {mapFailure(redigerStatus, (error) => (
        <ApiErrorAlert>Kunne ikke åpne utfallet for redigering: {error.detail}</ApiErrorAlert>
      ))}
      {harTilgang ? (
        <BehandlingHandlingKnapper>
          <Button onClick={next} disabled={!kanGaaVidere}>
            Neste
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </VStack>
  )
}
