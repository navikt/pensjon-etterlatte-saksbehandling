import { useAppSelector } from '~store/Store'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'

export function useBehandling(): IBehandlingReducer | null {
  return useAppSelector((state) => state.behandlingReducer.behandling)
}
