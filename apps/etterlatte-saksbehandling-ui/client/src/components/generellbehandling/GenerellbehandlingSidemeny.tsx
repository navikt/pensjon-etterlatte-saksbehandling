import { Sidebar } from '~shared/components/Sidebar'
import { Generellbehandling, KravpakkeUtland, Status } from '~shared/types/Generellbehandling'
import { AttesteringMedUnderkjenning } from '~components/generellbehandling/AttesteringMedUnderkjenning'

const kanAttestere = (status: Status) => {
  return status === Status.FATTET
}
export const GenerellbehandlingSidemeny = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props

  const attesterbar = kanAttestere(utlandsBehandling.status)

  switch (utlandsBehandling.status) {
    case Status.OPPRETTET:
      break
    case Status.FATTET:
      break
    case Status.ATTESTERT:
      break
    case Status.AVBRUTT:
      break
  }
  return <Sidebar>{attesterbar && <AttesteringMedUnderkjenning utlandsBehandling={utlandsBehandling} />}</Sidebar>
}
