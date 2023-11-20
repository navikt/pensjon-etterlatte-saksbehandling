import { Sidebar } from '~shared/components/Sidebar'
import { Generellbehandling, KravpakkeUtland, Status } from '~shared/types/Generellbehandling'
import { AttesteringMedUnderkjenning } from '~components/generellbehandling/AttesteringMedUnderkjenning'

export const GenerellbehandlingSidemeny = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  const genererSidemeny = () => {
    switch (utlandsBehandling.status) {
      case Status.OPPRETTET:
        return null
      case Status.FATTET:
        return <AttesteringMedUnderkjenning utlandsBehandling={utlandsBehandling} />
      case Status.ATTESTERT:
        return null
      case Status.AVBRUTT:
        return null
    }
  }
  return <Sidebar>{genererSidemeny()}</Sidebar>
}
