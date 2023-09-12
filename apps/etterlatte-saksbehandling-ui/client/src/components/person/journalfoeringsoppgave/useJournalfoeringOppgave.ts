import { useAppSelector } from '~store/Store'
import { IJournalfoeringOppgaveReducer } from '~store/reducers/JournalfoeringOppgaveReducer'

export function useJournalfoeringOppgave(): IJournalfoeringOppgaveReducer {
  return useAppSelector((state) => state.journalfoeringOppgaveReducer)
}
