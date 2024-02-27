import React from 'react'
import { Klage, teksterKlageutfall } from '~shared/types/Klage'
import { BodyShort, Heading } from '@navikt/ds-react'
import { formaterStringTidspunktTimeMinutter } from '~utils/formattering'

export const InitiellVurderingVisning = (props: { klage: Klage }) => {
  const klage = props.klage

  return (
    <>
      <Heading level="2" size="medium">
        Initiell vurdering
      </Heading>
      <InitiellVurderingVisningContent klage={klage} />
    </>
  )
}

export const InitiellVurderingVisningContent = (props: { klage: Klage }) => {
  const klage = props.klage

  return (
    <dl>
      <Heading size="small" spacing>
        Utfall
      </Heading>
      <BodyShort spacing>{teksterKlageutfall[klage.initieltUtfall?.utfallMedBegrunnelse.utfall ?? 'UKJENT']}</BodyShort>
      <Heading size="small" spacing>
        Begrunnelse
      </Heading>
      <BodyShort spacing>{klage.initieltUtfall?.utfallMedBegrunnelse.begrunnelse || 'Ikke registrert'}</BodyShort>
      <Heading size="small" spacing>
        Saksbehandler
      </Heading>
      <BodyShort spacing>{klage.initieltUtfall?.saksbehandler ?? 'Ukjent'}</BodyShort>
      <Heading size="small" spacing>
        Tidspunkt
      </Heading>
      <BodyShort spacing>
        {klage.initieltUtfall?.tidspunkt
          ? formaterStringTidspunktTimeMinutter(klage.initieltUtfall.tidspunkt)
          : 'Ingen tidspunkt'}
      </BodyShort>
    </dl>
  )
}
