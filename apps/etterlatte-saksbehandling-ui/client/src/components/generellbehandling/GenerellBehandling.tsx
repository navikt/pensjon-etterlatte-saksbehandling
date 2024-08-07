import { useParams } from 'react-router-dom'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGenerellBehandling } from '~shared/api/generellbehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import KravpakkeUtlandBehandling from '~components/generellbehandling/KravpakkeUtlandBehandling'
import { KravpakkeUtland } from '~shared/types/Generellbehandling'
import { Alert } from '@navikt/ds-react'
import { Generellbehandling } from '~shared/types/Generellbehandling'
import { StatusBarPersonHenter } from '~shared/statusbar/Statusbar'
import { hentSak } from '~shared/api/sak'
import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useSidetittel } from '~shared/hooks/useSidetittel'

const GenerellBehandling = () => {
  useSidetittel('Generell behandling')

  const { generellbehandlingId } = useParams()
  if (!generellbehandlingId) return null

  const [fetchGenerellbehandlingStatus, fetchGenerellbehandling] = useApiCall(hentGenerellBehandling)
  const [hentSakStatus, hentSakApi] = useApiCall(hentSak)

  useEffect(() => {
    fetchGenerellbehandling(generellbehandlingId)
  }, [generellbehandlingId])

  useEffect(() => {
    if (isSuccess(fetchGenerellbehandlingStatus)) {
      hentSakApi(fetchGenerellbehandlingStatus.data.sakId)
    }
  }, [fetchGenerellbehandlingStatus])

  return mapApiResult(
    fetchGenerellbehandlingStatus,
    <Spinner visible={true} label="Henter generell behandling" />,
    () => <ApiErrorAlert>Kunne ikke hente generell behandling</ApiErrorAlert>,
    (generellBehandling) => {
      switch (generellBehandling.type) {
        case 'KRAVPAKKE_UTLAND':
          return (
            <>
              {isFailureHandler({
                apiResult: hentSakStatus,
                errorMessage: 'Vi klarte ikke å hente sak og derfor vil navn baren være borte',
              })}
              {isSuccess(hentSakStatus) && (
                <StatusBarPersonHenter ident={hentSakStatus.data.ident} saksId={hentSakStatus.data.id} />
              )}
              <KravpakkeUtlandBehandling
                utlandsBehandling={generellBehandling as Generellbehandling & { innhold: KravpakkeUtland | null }}
              />
            </>
          )
        case 'ANNEN':
          return <Alert variant="error">Annen er ikke støttet enda</Alert>
        default:
          return <Alert variant="error">Ugyldig type {generellBehandling.type}</Alert>
      }
    }
  )
}

export default GenerellBehandling
