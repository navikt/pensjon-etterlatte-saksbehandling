import { KlageVurderingVisning } from '~components/klage/vurdering/KlageVurderingVisning'
import React from 'react'
import { Klage } from '~shared/types/Klage'
import { Heading } from '@navikt/ds-react'
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
            <dt>Utfall</dt>
            <dd>{klage.initieltUtfall.utfallMedBegrunnelse.utfall}</dd>
            <dt>Begrunnelse</dt>
            <dd>{klage.initieltUtfall.utfallMedBegrunnelse.begrunnelse}</dd>
            <dt>Saksbehandler</dt>
            <dd>{klage.initieltUtfall.saksbehandler}</dd>
            <dt>Tidspunkt</dt>
            <dd>{formaterStringTidspunktTimeMinutter(klage.initieltUtfall.tidspunkt)}</dd>
          </dl>
        </>
      )}
    </>
  )
}
