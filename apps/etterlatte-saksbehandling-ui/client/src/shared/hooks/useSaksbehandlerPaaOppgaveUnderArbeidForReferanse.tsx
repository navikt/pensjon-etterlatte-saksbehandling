import { useEffect } from 'react'
import {
  resetSaksbehandlerGjeldendeOppgave,
  setSaksbehandlerGjeldendeOppgave,
} from '~store/reducers/SaksbehandlerGjeldendeOppgaveForBehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSaksbehandlerForOppgaveUnderBehandling } from '~shared/api/oppgaver'
import { useAppDispatch } from '~store/Store'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { Result } from '~shared/api/apiUtils'

export const useSaksbehandlerPaaOppgaveUnderArbeidForReferanse = ({
  referanse,
}: {
  referanse: string
  sakId: number
}): [Result<Saksbehandler>, () => void] => {
  const dispatch = useAppDispatch()
  const [saksbehandlerForOppgaveResult, hentSaksbehandlerForOppgave] = useApiCall(
    hentSaksbehandlerForOppgaveUnderBehandling
  )

  const hentSaksbehandlerWrapper = () => {
    hentSaksbehandlerForOppgave(
      referanse,
      (saksbehandler, statusCode) => {
        if (statusCode === 200 && !!saksbehandler) {
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
