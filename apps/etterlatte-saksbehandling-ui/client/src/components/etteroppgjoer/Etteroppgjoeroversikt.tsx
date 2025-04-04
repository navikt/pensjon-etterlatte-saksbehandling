import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { OversiktOverEtteroppgjoer } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/OversiktOverEtteroppgjoer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import React, { useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const Etteroppgjoeroversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const etteroppgjoerId = behandling.relatertBehandlingId
  const dispatch = useAppDispatch()
  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoer)

  useEffect(() => {
    if (!etteroppgjoerId) return
    hentEtteroppgjoerRequest(etteroppgjoerId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerId])

  return mapResult(etteroppgjoerResult, {
    pending: <Spinner label="Henter etteroppgjørbehandling" />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente forbehandlingen for etteroppgjør: {error.detail}</ApiErrorAlert>,
    success: () => <OversiktOverEtteroppgjoer />,
  })
}
