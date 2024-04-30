import { useAppSelector } from '~store/Store'
import { OppgaveDTO } from '~shared/types/oppgave'

export function useSelectorOppgaveUnderBehandling(): OppgaveDTO | null {
  return useAppSelector((state) => state.oppgaveReducer.oppgave)
}
