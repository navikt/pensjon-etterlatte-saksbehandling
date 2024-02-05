import { KlageInitiellVurdering } from '~components/klage/vurdering/KlageInitiellVurdering'
import { KlageInitiellVurderingVisning } from '~components/klage/vurdering/KlageInitiellVurderingVisning'
import { useKlage } from '~components/klage/useKlage'
import Spinner from '~shared/Spinner'
import React from 'react'

export function KlageVurdering({ kanRedigere }: { kanRedigere: boolean }) {
  const klage = useKlage()
  if (!klage) {
    return <Spinner visible label="Henter klage" />
  }
  return kanRedigere ? <KlageInitiellVurdering klage={klage} /> : <KlageInitiellVurderingVisning klage={klage} />
}
