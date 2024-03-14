import React from 'react'
import { BodyShort, Heading } from '@navikt/ds-react'
import { formaterKlageutfall, VisInnstilling, VisOmgjoering } from '~components/klage/vurdering/KlageVurderingFelles'
import { Klage } from '~shared/types/Klage'

export function EndeligVurderingVisning(props: { klage: Klage }) {
  const klage = props.klage

  const { utfall, sak } = klage
  return (
    <>
      <Heading level="2" size="medium">
        Endelig utfall
      </Heading>
      {utfall ? (
        <>
          <BodyShort spacing>
            Utfallet av klagen er <strong>{formaterKlageutfall(klage)}</strong>.
          </BodyShort>

          {utfall?.utfall === 'DELVIS_OMGJOERING' || utfall?.utfall === 'STADFESTE_VEDTAK' ? (
            <VisInnstilling innstilling={utfall.innstilling} sakId={sak?.id} kanRedigere={false} />
          ) : null}

          {utfall?.utfall === 'DELVIS_OMGJOERING' ||
          utfall?.utfall === 'OMGJOERING' ||
          utfall?.utfall === 'AVVIST_MED_OMGJOERING' ? (
            <VisOmgjoering omgjoering={utfall.omgjoering} kanRedigere={false} />
          ) : null}
        </>
      ) : (
        <>
          <BodyShort>Ikke registrert</BodyShort>
        </>
      )}
    </>
  )
}
