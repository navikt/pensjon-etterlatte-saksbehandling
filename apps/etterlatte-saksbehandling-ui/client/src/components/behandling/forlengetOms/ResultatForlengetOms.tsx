import { useContext } from 'react'
import { Alert, BodyShort, Box, Button, Label, Radio, RadioGroup, Textarea, VStack } from '@navikt/ds-react'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useForlengetOms } from './ForlengetOmsContext'
import { ForlengetOmsUtfall } from '~shared/api/forlengetOms'
import { isPending } from '@reduxjs/toolkit'

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
    alleVilkaarErVurdert,
    lagretUtfallErUtdatert,
    kanLagre,
    kanVelgeUtfall,
    kanGaaVidere,
    lagreStatus,
  } = useForlengetOms()
  const { next } = useContext(BehandlingRouteContext)

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
          {!alleVilkaarErVurdert && <BodyShort>Alle vilkår må vurderes før man kan gå videre.</BodyShort>}
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
      {lagretUtfallErUtdatert && (
        <Alert variant="warning">
          Virkningstidspunktet eller grunnlaget er endret. Velg «Rediger vurdering» og vurder utfallet på nytt.
        </Alert>
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
