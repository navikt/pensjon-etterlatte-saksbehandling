import { FlexRow } from '~shared/styled'
import React from 'react'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { useNavigate } from 'react-router-dom'
import { forrigeSteg, nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { formaterKlageutfall, VisInnstilling, VisOmgjoering } from '~components/klage/vurdering/KlageVurderingFelles'
import { Klage } from '~shared/types/Klage'

export function EndeligVurderingVisning(props: { klage: Klage }) {
  const klage = props.klage

  const navigate = useNavigate()

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
          <FlexRow justify="center">
            <Button className="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'vurdering'))}>
              Gå tilbake
            </Button>
            <Button className="button" variant="primary" onClick={() => navigate(nesteSteg(klage, 'vurdering'))}>
              Neste side
            </Button>
          </FlexRow>
        </>
      ) : (
        <BodyShort>Ikke registrert</BodyShort>
      )}
    </>
  )
}
