import { KlageVurdering } from '~components/klage/vurdering/KlageVurdering'
import { KlageVurderingVisning } from '~components/klage/vurdering/KlageVurderingVisning'

export function KlageVurderingEntry({ redigerbar }: { redigerbar: boolean | null }) {
  return redigerbar ? <KlageVurdering /> : <KlageVurderingVisning />
}
