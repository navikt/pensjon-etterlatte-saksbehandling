import { IDetaljertBehandling, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { BosattUtland } from '~components/behandling/soeknadsoversikt/bosattUtland/BosattUtland'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBosattutland } from '~shared/api/bosattutland'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { Alert, Heading } from '@navikt/ds-react'

import { isFailureWithCode, isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'

export const SkalViseBosattUtland = (props: { behandling: IDetaljertBehandling; redigerbar: boolean }) => {
  const { behandling, redigerbar } = props
  return (
    <>
      {behandling.utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND && (
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
      <Heading level="2" size="medium" style={{ marginTop: '2rem' }} spacing>
        Mottatt krav fra utland <EessiPensjonLenke />
      </Heading>

      {isPending(hentBosattUtlandStatus) && <Spinner visible={true} label="Henter bosatt utland info" />}
      {isFailureWithCode(hentBosattUtlandStatus, 404) && (
        <BosattUtland behandlingId={behandlingId} bosattutland={null} redigerbar={redigerbar} />
      )}
      {isSuccess(hentBosattUtlandStatus) && (
        <BosattUtland behandlingId={behandlingId} bosattutland={hentBosattUtlandStatus.data} redigerbar={redigerbar} />
      )}
      {isFailure(hentBosattUtlandStatus) && !isFailureWithCode(hentBosattUtlandStatus, 404) && (
        <Alert variant="warning">Vi klarte ikke å hente lagret data for bosatt utland</Alert>
      )}
    </>
  )
}
