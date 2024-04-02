import { useEffect } from 'react'
import {
  resetSaksbehandlerGjeldendeOppgave,
  setSaksbehandlerGjeldendeOppgave,
} from '~store/reducers/SaksbehandlerGjeldendeOppgaveForBehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSaksbehandlerForReferanseOppgaveUnderArbeid } from '~shared/api/oppgaver'
import { useAppDispatch } from '~store/Store'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { Result } from '~shared/api/apiUtils'

export const useGjeldendeSaksbehandlerForReferanse = ({
  referanse,
  sakId,
}: {
  referanse: string
  sakId: number
}): [Result<Saksbehandler>, () => void] => {
  const dispatch = useAppDispatch()
  const [saksbehandlerForOppgaveResult, hentSaksbehandlerForOppgave] = useApiCall(
    hentSaksbehandlerForReferanseOppgaveUnderArbeid
  )
  const hentSaksbehandlerWrapper = () => {
    hentSaksbehandlerForOppgave(
      { referanse: referanse, sakId: sakId },
      (saksbehandler, statusCode) => {
        if (statusCode === 200) {
          dispatch(setSaksbehandlerGjeldendeOppgave(saksbehandler.ident))
        }
      },
      () => dispatch(resetSaksbehandlerGjeldendeOppgave())
    )
  }
  useEffect(() => {
    hentSaksbehandlerWrapper()
  }, [referanse])

  return [saksbehandlerForOppgaveResult, hentSaksbehandlerWrapper]
}
