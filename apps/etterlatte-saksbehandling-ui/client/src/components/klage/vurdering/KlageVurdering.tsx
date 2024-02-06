import { KlageVurderingRedigering } from '~components/klage/vurdering/KlageVurderingRedigering'
import { KlageVurderingVisning } from '~components/klage/vurdering/KlageVurderingVisning'
import { useKlage } from '~components/klage/useKlage'
import Spinner from '~shared/Spinner'
import React from 'react'
import { JaNei } from '~shared/types/ISvar'
import { KlageInfoInnhenting } from '~components/klage/vurdering/innhenting/KlageInfoInnhenting'

export function KlageVurdering({ kanRedigere }: { kanRedigere: boolean }) {
  const klage = useKlage()

  if (!klage) {
    return <Spinner visible label="Henter klage" />
  }

  if (klage.formkrav?.formkrav.erFormkraveneOppfylt === JaNei.NEI) {
    return <KlageInfoInnhenting klage={klage} />
  }

  return kanRedigere ? <KlageVurderingRedigering klage={klage} /> : <KlageVurderingVisning klage={klage} />
}
