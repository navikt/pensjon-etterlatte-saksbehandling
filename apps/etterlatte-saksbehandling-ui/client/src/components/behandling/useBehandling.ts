import { useAppSelector } from '~store/Store'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'

export function useBehandling(): IBehandlingReducer | null {
  return useAppSelector((state) => state.behandlingReducer.behandling)
}
