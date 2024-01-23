import { KlageFormkravRedigering } from '~components/klage/formkrav/KlageFormkravRedigering'
import { KlageFormkravVisning } from '~components/klage/formkrav/KlageFormkravVisning'

export function KlageFormkrav({ redigerbar, kanRedigere }: { redigerbar: boolean | null; kanRedigere: boolean }) {
  return redigerbar ? <KlageFormkravRedigering kanRedigere={kanRedigere} /> : <KlageFormkravVisning />
}
