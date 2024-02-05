import { KlageVurderingVisning } from '~components/klage/vurdering/KlageVurderingVisning'
import React from 'react'
import { Klage } from '~shared/types/Klage'

export const KlageInitiellVurderingVisning = (props: { klage: Klage }) => {
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
            <dd>{klage.initieltUtfall.tidspunkt}</dd>
          </dl>
        </>
      )}
      <KlageVurderingVisning klage={klage} />
    </>
  )
}
