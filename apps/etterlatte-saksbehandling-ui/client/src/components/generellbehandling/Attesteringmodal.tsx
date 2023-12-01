import { Alert, Button } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { attesterGenerellbehandling } from '~shared/api/generellbehandling'
import { hentSakOgNavigerTilSaksoversikt } from '~components/generellbehandling/KravpakkeUtland'
import { useNavigate } from 'react-router-dom'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'

import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'

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
      {isSuccess(attesterStatus) && <Alert variant="success">Behandlingen ble attestert</Alert>}
      {isFailure(attesterStatus) && <Alert variant="error">Behandlingen ble ikke attestert</Alert>}
    </>
  )
}
