import { KlageInitiellVurdering } from '~components/klage/vurdering/KlageInitiellVurdering'
import { KlageInitiellVurderingVisning } from '~components/klage/vurdering/KlageInitiellVurderingVisning'
import { useKlage } from '~components/klage/useKlage'
import Spinner from '~shared/Spinner'
import React from 'react'
import { JaNei } from '~shared/types/ISvar'
import { KlageInfoInnhenting } from '~components/klage/vurdering/innhenting/KlageInfoInnhenting'
import { KlageAvvisningRedigering } from '~components/klage/vurdering/KlageAvvisningRedigering'

export function KlageVurdering({ kanRedigere }: { kanRedigere: boolean }) {
  const klage = useKlage()
  if (!klage) {
    return <Spinner visible label="Henter klage" />
  }

  if (klage.formkrav?.formkrav.erKlagenFramsattInnenFrist === JaNei.NEI) {
    return <KlageAvvisningRedigering klage={klage} />
  }

  if (klage.formkrav?.formkrav.erFormkraveneOppfylt === JaNei.NEI) {
    return <KlageInfoInnhenting klage={klage} />
  }

  return kanRedigere ? <KlageInitiellVurdering klage={klage} /> : <KlageInitiellVurderingVisning klage={klage} />
}
