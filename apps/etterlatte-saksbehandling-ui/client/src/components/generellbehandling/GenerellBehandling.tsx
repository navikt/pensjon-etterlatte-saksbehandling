import { useParams } from 'react-router-dom'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGenerellBehandling } from '~shared/api/generellbehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import KravpakkeUtland from '~components/generellbehandling/KravpakkeUtland'
import { Alert } from '@navikt/ds-react'
import { Generellbehandling } from '~shared/types/Generellbehandling'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { getPerson } from '~shared/api/grunnlag'
import { hentSak } from '~shared/api/sak'

import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const GenerellBehandling = () => {
  const { generellbehandlingId } = useParams()
  if (!generellbehandlingId) return null

  const [fetchGenerellbehandlingStatus, fetchGenerellbehandling] = useApiCall(hentGenerellBehandling)
  const [personStatus, hentPerson] = useApiCall(getPerson)
  const [hentSakStatus, hentSakApi] = useApiCall(hentSak)

  useEffect(() => {
    fetchGenerellbehandling(generellbehandlingId)
  }, [generellbehandlingId])

  useEffect(() => {
    if (isSuccess(fetchGenerellbehandlingStatus)) {
      hentSakApi(fetchGenerellbehandlingStatus.data.sakId)
    }
  }, [fetchGenerellbehandlingStatus])

  useEffect(() => {
    if (isSuccess(hentSakStatus)) {
      hentPerson(hentSakStatus.data.ident)
    }
  }, [hentSakStatus])

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
              <StatusBar result={personStatus} />
              <KravpakkeUtland
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
