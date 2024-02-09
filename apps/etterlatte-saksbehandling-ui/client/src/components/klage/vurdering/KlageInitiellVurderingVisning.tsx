import { KlageVurderingVisning } from '~components/klage/vurdering/KlageVurderingVisning'
import React from 'react'
import { Klage, teksterKlageutfall } from '~shared/types/Klage'
import { BodyShort, Heading } from '@navikt/ds-react'
import { formaterStringTidspunktTimeMinutter } from '~utils/formattering'

export const KlageInitiellVurderingVisning = (props: { klage: Klage }) => {
  const klage = props.klage
  return (
    <>
      <Heading level="2" size="large">
        Initiell vurdering
      </Heading>
      <InitiellVurderingVisning klage={klage} />
      <KlageVurderingVisning klage={klage} />
    </>
  )
}

export const InitiellVurderingVisning = (props: { klage: Klage }) => {
  const klage = props.klage
  return (
    <>
      {klage.initieltUtfall && (
        <>
          <dl>
            <Heading size="small" spacing>
              Utfall
            </Heading>
            <BodyShort spacing>{teksterKlageutfall[klage.initieltUtfall.utfallMedBegrunnelse.utfall]}</BodyShort>
            <Heading size="small" spacing>
              Begrunnelse
            </Heading>
            <BodyShort spacing>{klage.initieltUtfall.utfallMedBegrunnelse.begrunnelse || 'Ikke registrert'}</BodyShort>
            <Heading size="small" spacing>
              Saksbehandler
            </Heading>
            <BodyShort spacing>{klage.initieltUtfall.saksbehandler}</BodyShort>
            <Heading size="small" spacing>
              Tidspunkt
            </Heading>
            <BodyShort spacing>{formaterStringTidspunktTimeMinutter(klage.initieltUtfall.tidspunkt)}</BodyShort>
          </dl>
        </>
      )}
    </>
  )
}
