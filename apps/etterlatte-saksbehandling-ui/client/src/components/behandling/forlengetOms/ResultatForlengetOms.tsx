import { useContext, useState } from 'react'
import { Alert, BodyShort, Box, Button, Label, Radio, RadioGroup, Textarea, VStack } from '@navikt/ds-react'
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
    aktivitetType,
    aktivitetUtdanning,
    arbeidsrettetTiltak,
    periode,
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
    kanGaaVidere,
    lagreStatus,
  } = useForlengetOms()
  const { next } = useContext(BehandlingRouteContext)
  const behandling = useBehandling()
  const virkningstidspunktSamsvarer = behandling?.virkningstidspunkt?.dato === vilkaarsvurdering?.virkningstidspunkt
  const [valideringsfeil, settValideringsfeil] = useState<string[]>([])

  const validerOgLagre = () => {
    if (!kanRedigere) return
    const feil: string[] = []
    if (!aktivitetType) {
      feil.push('Velg om bruker tar utdanning eller deltar i arbeidsrettet tiltak.')
    } else if (
      aktivitetType === 'UTDANNING' &&
      (!aktivitetUtdanning.skole.trim() ||
        !aktivitetUtdanning.studium.trim() ||
        !aktivitetUtdanning.studererMinst50Prosent)
    ) {
      feil.push('Fyll ut skole, studium og om bruker studerer minst 50 prosent.')
    } else if (
      aktivitetType === 'ARBEIDSRETTET_TILTAK' &&
      (!arbeidsrettetTiltak.tiltak.trim() || !arbeidsrettetTiltak.omfang || !arbeidsrettetTiltak.aktivitetsplan)
    ) {
      feil.push('Fyll ut tiltak, omfang og om bruker har en aktivitetsplan.')
    }
    if (!utfall) feil.push('Velg om forlenget OMS innvilges eller avslås.')
    if (!begrunnelse.trim()) feil.push('Oppgi en begrunnelse.')
    if (utfall === 'INNVILGELSE') {
      if (!periode.fom || !periode.tom) {
        feil.push('Innvilgelse krever en fra og med- og til og med-måned.')
      } else if (periode.tom < periode.fom) {
        feil.push('Til og med-måneden må være lik eller etter fra og med-måneden.')
      }
    }
    settValideringsfeil(feil)
    if (feil.length === 0) lagreVurdering()
  }

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
              disabled={!kanRedigere}
            >
              <Radio value="INNVILGELSE">Innvilg forlenget OMS</Radio>
              <Radio value="AVSLAG">Avslå forlenget OMS</Radio>
            </RadioGroup>
          )}
          {harTilgang && !vilkaarsvurdering?.resultat && (
            <Box>
              <Button
                size="small"
                onClick={validerOgLagre}
                loading={isPending(lagreStatus) || isPending(redigerStatus)}
                disabled={!kanRedigere}
              >
                Lagre
              </Button>
              {valideringsfeil.length > 0 && (
                <Alert variant="error">
                  <ul>
                    {valideringsfeil.map((feil) => (
                      <li key={feil}>{feil}</li>
                    ))}
                  </ul>
                </Alert>
              )}
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
