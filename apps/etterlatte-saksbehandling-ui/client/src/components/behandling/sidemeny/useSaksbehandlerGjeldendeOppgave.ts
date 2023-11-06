import { useAppSelector } from '~store/Store'

export function useSaksbehandlerGjeldendeOppgave(): string | null {
  return useAppSelector((state) => state.saksbehandlerGjeldendeOppgaveReducer.saksbehandler)
}
