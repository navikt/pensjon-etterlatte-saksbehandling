import { useEffect } from 'react'
import { resetOppgave, settOppgave } from '~store/reducers/OppgaveReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAttesterbarBehandlingOppgaveForReferanse } from '~shared/api/oppgaver'
import { useAppDispatch } from '~store/Store'
import { Result } from '~shared/api/apiUtils'
import { OppgaveDTO } from '~shared/types/oppgave'

export const useAttesterbarBehandlingOppgave = ({
  referanse,
}: {
  referanse: string
}): [Result<OppgaveDTO>, () => void] => {
  const dispatch = useAppDispatch()

  const [oppgaveResult, hentOppgave] = useApiCall(hentAttesterbarBehandlingOppgaveForReferanse)

  const hentOppgaveWrapper = () => {
    hentOppgave(
      referanse,
      (oppgave, statusCode) => {
        if (statusCode === 200 && !!oppgave) {
          dispatch(settOppgave(oppgave))
        }
      },
      () => dispatch(resetOppgave())
    )
  }

  useEffect(() => {
    hentOppgaveWrapper()
  }, [referanse])

  return [oppgaveResult, hentOppgaveWrapper]
}
