import { KlageFormkravRedigering } from '~components/klage/formkrav/KlageFormkravRedigering'
import { KlageFormkravVisning } from '~components/klage/formkrav/KlageFormkravVisning'

export function KlageFormkrav({ kanRedigere }: { kanRedigere: boolean }) {
  return kanRedigere ? <KlageFormkravRedigering /> : <KlageFormkravVisning />
}
