import { useAppSelector } from '~store/Store'
import { InnloggetSaksbehandler } from '~shared/types/saksbehandler'

export function useInnloggetSaksbehandler(): InnloggetSaksbehandler {
  return useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
}
