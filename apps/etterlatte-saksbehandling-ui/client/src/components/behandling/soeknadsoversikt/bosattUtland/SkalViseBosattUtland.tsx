import { IDetaljertBehandling, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { BosattUtland } from '~components/behandling/soeknadsoversikt/bosattUtland/BosattUtland'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBosattutland } from '~shared/api/bosattutland'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { Alert, Heading } from '@navikt/ds-react'
import { mapResult } from '~shared/api/apiUtils'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'

export const SkalViseBosattUtland = (props: { behandling: IDetaljertBehandling; redigerbar: boolean }) => {
  const { behandling, redigerbar } = props
  return (
    <>
      {behandling.utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND && (
        <HentBosattutland behandling={behandling} redigerbar={redigerbar} />
      )}
    </>
  )
}

const HentBosattutland = ({ behandling, redigerbar }: { behandling: IDetaljertBehandling; redigerbar: boolean }) => {
  const [hentBosattUtlandStatus, hentBosattUtlandApi] = useApiCall(hentBosattutland)

  useEffect(() => {
    hentBosattUtlandApi(behandling.id)
  }, [])

  return (
    <>
      <Heading level="2" size="medium" style={{ marginTop: '2rem' }} spacing>
        Mottatt krav fra utland{' '}
        <EessiPensjonLenke sakId={behandling.sakId} behandlingId={behandling.id} sakType={behandling.sakType} />
      </Heading>

      {mapResult(hentBosattUtlandStatus, {
        pending: <Spinner label="Henter bosatt utland info" />,
        error: (error) =>
          error.status === 404 ? (
            <BosattUtland behandlingId={behandling.id} bosattutland={null} redigerbar={redigerbar} />
          ) : (
            <Alert variant="warning">Vi klarte ikke å hente lagret data for bosatt utland</Alert>
          ),
        success: (bosattutland) => (
          <BosattUtland behandlingId={behandling.id} bosattutland={bosattutland} redigerbar={redigerbar} />
        ),
      })}
    </>
  )
}
