import { IDetaljertBehandling, UtenlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { BosattUtland } from '~components/behandling/soeknadsoversikt/bosattUtland/BosattUtland'
import { isErrorWithCode, isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentBosattutland } from '~shared/api/bosattutland'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { Alert } from '@navikt/ds-react'

export const SkalViseBosattUtland = (props: { behandling: IDetaljertBehandling; redigerbar: boolean }) => {
  const { behandling, redigerbar } = props
  return (
    <>
      {behandling.utenlandstilknytning?.type === UtenlandstilknytningType.BOSATT_UTLAND && (
        <HentBosattutland behandlingId={behandling.id} redigerbar={redigerbar} />
      )}
    </>
  )
}

const HentBosattutland = ({ behandlingId, redigerbar }: { behandlingId: string; redigerbar: boolean }) => {
  const [hentBosattUtlandStatus, hentBosattUtlandApi] = useApiCall(hentBosattutland)
  useEffect(() => {
    hentBosattUtlandApi(behandlingId)
  }, [])
  return (
    <>
      {isPending(hentBosattUtlandStatus) && <Spinner visible={true} label="Henter bosatt utland info" />}
      {isErrorWithCode(hentBosattUtlandStatus, 404) && (
        <BosattUtland behandlingId={behandlingId} bosattutland={null} redigerbar={redigerbar} />
      )}
      {isSuccess(hentBosattUtlandStatus) && (
        <BosattUtland behandlingId={behandlingId} bosattutland={hentBosattUtlandStatus.data} redigerbar={redigerbar} />
      )}
      {isFailure(hentBosattUtlandStatus) && !isErrorWithCode(hentBosattUtlandStatus, 404) && (
        <Alert variant="warning">Vi klarte ikke å hente lagret data for bosatt utland</Alert>
      )}
    </>
  )
}
