import { useAppSelector } from '~store/Store'
import { INyBehandlingReducer } from '~store/reducers/NyBehandlingReducer'

export function useNyBehandling(): INyBehandlingReducer {
  return useAppSelector((state) => state.nyBehandlingReducer)
}
