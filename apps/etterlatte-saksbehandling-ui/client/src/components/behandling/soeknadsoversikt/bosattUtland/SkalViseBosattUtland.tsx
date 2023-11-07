import { IDetaljertBehandling, UtenlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { BosattUtland } from '~components/behandling/soeknadsoversikt/bosattUtland/BosattUtland'

export const SkalViseBosattUtland = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  return (
    <>
      {behandling.utenlandstilknytning?.type === UtenlandstilknytningType.BOSATT_UTLAND && (
        <BosattUtland behandlingId={behandling.id} />
      )}
    </>
  )
}
