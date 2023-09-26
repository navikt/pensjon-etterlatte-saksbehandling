import { useParams } from 'react-router-dom'
import { useEffect } from 'react'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentGenerellBehandling } from '~shared/api/generellbehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import Utland from '~components/generellbehandling/Utland'
import { Alert } from '@navikt/ds-react'
import { Generellbehandling } from '~shared/types/Generellbehandling'

const GenerellBehandling = () => {
  const { generellbehandlingId } = useParams()
  const [fetchGenerellbehandlingStatus, fetchGenerellbehandling] = useApiCall(hentGenerellBehandling)
  if (!generellbehandlingId) return null

  useEffect(() => {
    fetchGenerellbehandling(generellbehandlingId)
  }, [generellbehandlingId])

  return mapApiResult(
    fetchGenerellbehandlingStatus,
    <Spinner visible={true} label="Henter generell behandling" />,
    () => <ApiErrorAlert>Kunne ikke hente generell behandling</ApiErrorAlert>,
    (generellBehandling) => {
      switch (generellBehandling.type) {
        case 'UTLAND':
          return <Utland utlandsBehandling={generellBehandling as Generellbehandling & { innhold: Utland }} />
        case 'ANNEN':
          return <Alert variant="error">Annen er ikke støttet enda</Alert>
        default:
          return <Alert variant="error">Ugyldig type {generellBehandling.type}</Alert>
      }
    }
  )
}

export default GenerellBehandling
