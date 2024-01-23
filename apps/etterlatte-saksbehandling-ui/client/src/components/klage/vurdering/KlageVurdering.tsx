import { KlageVurderingRedigering } from '~components/klage/vurdering/KlageVurderingRedigering'
import { KlageVurderingVisning } from '~components/klage/vurdering/KlageVurderingVisning'

export function KlageVurdering({ redigerbar }: { redigerbar: boolean | null }) {
  return redigerbar ? <KlageVurderingRedigering /> : <KlageVurderingVisning />
}
