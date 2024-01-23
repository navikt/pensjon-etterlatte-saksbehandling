import { KlageVurderingRedigering } from '~components/klage/vurdering/KlageVurderingRedigering'
import { KlageVurderingVisning } from '~components/klage/vurdering/KlageVurderingVisning'

export function KlageVurdering({ kanRedigere }: { kanRedigere: boolean | null }) {
  return kanRedigere ? <KlageVurderingRedigering /> : <KlageVurderingVisning />
}
