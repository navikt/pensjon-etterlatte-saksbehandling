import { KlageFormkrav } from '~components/klage/formkrav/KlageFormkrav'
import { KlageFormkravVisning } from '~components/klage/formkrav/KlageFormkravVisning'

export function KlageFormkravEntry({ redigerbar, kanRedigere }: { redigerbar: boolean | null; kanRedigere: boolean }) {
  return redigerbar ? <KlageFormkrav kanRedigere={kanRedigere} /> : <KlageFormkravVisning />
}
