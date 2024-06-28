import React from 'react'
import { Klage, teksterKlageutfall } from '~shared/types/Klage'
import { BodyShort, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

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
    <VStack gap="2">
      <HStack gap="4">
        <Info label="Utfall" tekst={utfall} />
        <Info label="Sist endret" tekst={sistEndret} />
        <Info label="Saksbehandler" tekst={saksbehandler} />
      </HStack>
      <Heading size="xsmall">Begrunnelse</Heading>
      <BodyShort spacing>{klage.initieltUtfall?.utfallMedBegrunnelse.begrunnelse || 'Ikke registrert'}</BodyShort>
    </VStack>
  )
}
