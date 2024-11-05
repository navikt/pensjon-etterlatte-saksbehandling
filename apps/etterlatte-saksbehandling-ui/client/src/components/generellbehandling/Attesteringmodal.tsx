import { Alert, Button } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { attesterGenerellbehandling } from '~shared/api/generellbehandling'
import { hentSakOgNavigerTilSaksoversikt } from '~components/generellbehandling/KravpakkeUtlandBehandling'
import { useNavigate } from 'react-router-dom'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export const Attesteringmodal = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  const [attesterStatus, attesterFetch] = useApiCall(attesterGenerellbehandling)
  const navigate = useNavigate()

  const attesterWrapper = () => {
    attesterFetch(utlandsBehandling, () => {
      setTimeout(() => {
        hentSakOgNavigerTilSaksoversikt(utlandsBehandling.sakId, navigate)
      }, 4000)
    })
  }
  return (
    <>
      <Button style={{ marginTop: '1rem' }} onClick={() => attesterWrapper()} loading={isPending(attesterStatus)}>
        Godkjenn kravpakken
      </Button>
      {isSuccess(attesterStatus) && (
        <Alert variant="success">Behandlingen ble attestert, vi sender deg straks til saksoversikten</Alert>
      )}
      {isFailureHandler({
        apiResult: attesterStatus,
        errorMessage: 'Behandlingen ble ikke attestert',
      })}
    </>
  )
}
