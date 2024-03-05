import { BodyShort, Label, VStack } from '@navikt/ds-react'
import {
  teksterTilbakekrevingAarsak,
  teksterTilbakekrevingAktsomhet,
  teksterTilbakekrevingHjemmel,
  TilbakekrevingAktsomhet,
  TilbakekrevingBehandling,
  TilbakekrevingVurdering,
} from '~shared/types/Tilbakekreving'
import React, { useState } from 'react'
import { InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'

export function TilbakekrevingVurderingOverordnetVisning({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const [vurdering] = useState<TilbakekrevingVurdering>(behandling.tilbakekreving.vurdering)

  return (
    <InnholdPadding>
      <VStack gap="8" style={{ width: '30em' }}>
        <Tekstfelt label="Årsak" value={teksterTilbakekrevingAarsak[vurdering.aarsak!!]} />
        <Tekstfelt label="Beskriv feilutbetalingen" value={vurdering.beskrivelse} />
        <Tekstfelt
          label="Vurder uaktsomhet"
          value={teksterTilbakekrevingAktsomhet[vurdering.aktsomhet.aktsomhet!!]}
        />

        {vurdering.aktsomhet.aktsomhet === TilbakekrevingAktsomhet.GROV_UAKTSOMHET && (
          <Tekstfelt
            label="Gjør en strafferettslig vurdering (Valgfri)"
            value={vurdering.aktsomhet.strafferettsligVurdering}
          />
        )}
        {vurdering.aktsomhet.aktsomhet &&
          [TilbakekrevingAktsomhet.SIMPEL_UAKTSOMHET, TilbakekrevingAktsomhet.GROV_UAKTSOMHET].includes(
            vurdering.aktsomhet.aktsomhet
          ) && (
            <>
              <Tekstfelt label="Redusering av kravet" value={vurdering.aktsomhet.reduseringAvKravet} />
              <Tekstfelt label="Rentevurdering" value={vurdering.aktsomhet.rentevurdering} />
            </>
          )}
        <Tekstfelt label="Konklusjon" value={vurdering.konklusjon} />
        <Tekstfelt label="Hjemmel" value={teksterTilbakekrevingHjemmel[vurdering.hjemmel!!]} />
      </VStack>
    </InnholdPadding>
  )
}

function Tekstfelt({ label, value }: { label: String; value?: String | null }) {
  return (
    <VStack gap="2">
      <Label>{label}</Label>
      <BodyShort>{value ?? ''}</BodyShort>
    </VStack>
  )
}
