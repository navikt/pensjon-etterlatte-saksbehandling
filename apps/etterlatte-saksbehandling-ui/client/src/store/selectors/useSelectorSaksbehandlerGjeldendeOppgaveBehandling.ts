import { useAppSelector } from '~store/Store'

export function useSelectorSaksbehandlerGjeldendeOppgaveBehandling(): string | null {
  return useAppSelector((state) => state.saksbehandlerGjeldendeOppgaveReducer.saksbehandler)
}
