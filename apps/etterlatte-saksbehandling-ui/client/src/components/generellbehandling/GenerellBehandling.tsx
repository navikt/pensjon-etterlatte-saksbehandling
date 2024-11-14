import { useParams } from 'react-router-dom'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGenerellBehandling } from '~shared/api/generellbehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import KravpakkeUtlandBehandling from '~components/generellbehandling/KravpakkeUtlandBehandling'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { Alert } from '@navikt/ds-react'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { hentSak } from '~shared/api/sak'
import { mapApiResult, mapResult } from '~shared/api/apiUtils'
import { useSidetittel } from '~shared/hooks/useSidetittel'

const GenerellBehandling = () => {
  useSidetittel('Generell behandling')

  const { generellbehandlingId } = useParams()
  if (!generellbehandlingId) return null

  const [fetchGenerellbehandlingStatus, fetchGenerellbehandling] = useApiCall(hentGenerellBehandling)
  const [hentSakStatus, hentSakApi] = useApiCall(hentSak)

  useEffect(() => {
    fetchGenerellbehandling(generellbehandlingId, (generellBehandling) => {
      hentSakApi(generellBehandling.sakId)
    })
  }, [generellbehandlingId])

  return mapApiResult(
    fetchGenerellbehandlingStatus,
    <Spinner label="Henter generell behandling" />,
    (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente generell behandling'}</ApiErrorAlert>,
    (generellBehandling) => {
      switch (generellBehandling.type) {
        case 'KRAVPAKKE_UTLAND':
          return (
            <>
              {mapResult(hentSakStatus, {
                error: (error) => <ApiErrorAlert>Feil ved henting av saksinformasjon: {error.detail}</ApiErrorAlert>,
                success: (sak) => <StatusBar ident={sak.ident} />,
              })}

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
