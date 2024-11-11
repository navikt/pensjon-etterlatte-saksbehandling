import { Alert, Button } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { attesterGenerellbehandling } from '~shared/api/generellbehandling'
import { hentSakOgNavigerTilSaksoversikt } from '~components/generellbehandling/KravpakkeUtlandBehandling'
import { useNavigate } from 'react-router-dom'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'

import { isPending, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

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

      {mapResult(attesterStatus, {
        error: (error) => <ApiErrorAlert>Behandlingen ble ikke attestert: {error.detail}</ApiErrorAlert>,
        success: () => (
          <Alert variant="success">Behandlingen ble attestert, du blir straks sendt til saksoversikten</Alert>
        ),
      })}
    </>
  )
}
