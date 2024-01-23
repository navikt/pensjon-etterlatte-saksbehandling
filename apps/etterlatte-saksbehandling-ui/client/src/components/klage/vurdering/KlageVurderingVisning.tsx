import { useKlage } from '~components/klage/useKlage'
import { Content, ContentHeader } from '~shared/styled'
import React from 'react'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { BodyLong, BodyShort, Heading } from '@navikt/ds-react'
import { TEKSTER_AARSAK_OMGJOERING, TEKSTER_LOVHJEMLER, teksterKlageutfall, Utfall } from '~shared/types/Klage'

export function KlageVurderingVisning() {
  const klage = useKlage()

  if (!klage) return

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <Heading size="small" spacing>
          Utfall
        </Heading>
        <BodyShort spacing>{klage.utfall?.utfall && teksterKlageutfall[klage.utfall?.utfall]}</BodyShort>
        {(klage.utfall?.utfall === 'DELVIS_OMGJOERING' || klage.utfall?.utfall === 'STADFESTE_VEDTAK') && (
          <>
            <Heading size="medium" spacing>
              Innstilling til KA
            </Heading>
            <Heading size="small" spacing>
              Hjemmel
            </Heading>
            <BodyShort spacing>{TEKSTER_LOVHJEMLER[klage.utfall.innstilling.lovhjemmel]}</BodyShort>
            <Heading size="small" spacing>
              Tekst
            </Heading>
            <BodyLong spacing>{klage.utfall.innstilling.tekst}</BodyLong>
          </>
        )}
        {(klage.utfall?.utfall === Utfall.OMGJOERING || klage.utfall?.utfall === Utfall.DELVIS_OMGJOERING) && (
          <>
            <Heading size="medium" spacing>
              Omgjøring
            </Heading>
            <Heading size="small" spacing>
              Årsak
            </Heading>
            <BodyShort spacing>{TEKSTER_AARSAK_OMGJOERING[klage.utfall.omgjoering.grunnForOmgjoering]}</BodyShort>
            <Heading size="small" spacing>
              Begrunnelse
            </Heading>
            <BodyLong spacing>{klage.utfall.omgjoering.begrunnelse}</BodyLong>
          </>
        )}
      </InnholdPadding>
    </Content>
  )
}
