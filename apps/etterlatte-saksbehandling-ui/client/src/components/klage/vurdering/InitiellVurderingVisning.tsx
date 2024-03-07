import React from 'react'
import { Klage, teksterKlageutfall } from '~shared/types/Klage'
import { BodyShort, Heading } from '@navikt/ds-react'
import { formaterDatoMedTidspunkt } from '~utils/formattering'
import { BredVurderingWrapper } from '~components/klage/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'

export const InitiellVurderingVisning = (props: { klage: Klage }) => {
  const klage = props.klage

  return (
    <>
      <Heading level="2" size="medium">
        Første vurdering
      </Heading>
      <InitiellVurderingVisningContent klage={klage} />
    </>
  )
}

export const InitiellVurderingVisningContent = (props: { klage: Klage }) => {
  const klage = props.klage

  const utfall = teksterKlageutfall[klage.initieltUtfall?.utfallMedBegrunnelse.utfall ?? 'UKJENT']
  const sistEndret = klage.initieltUtfall?.tidspunkt
    ? formaterDatoMedTidspunkt(new Date(klage.initieltUtfall.tidspunkt))
    : 'Ingen tidspunkt'
  const saksbehandler = klage.initieltUtfall?.saksbehandler ?? 'Ukjent'

  return (
    <BredVurderingWrapper>
      <FlexRow $spacing>
        <Info label="Utfall" tekst={utfall} />
        <Info label="Sist endret" tekst={sistEndret} />
        <Info label="Saksbehandler" tekst={saksbehandler} />
      </FlexRow>
      <Heading size="xsmall" spacing>
        Begrunnelse
      </Heading>
      <BodyShort spacing>{klage.initieltUtfall?.utfallMedBegrunnelse.begrunnelse || 'Ikke registrert'}</BodyShort>
    </BredVurderingWrapper>
  )
}
