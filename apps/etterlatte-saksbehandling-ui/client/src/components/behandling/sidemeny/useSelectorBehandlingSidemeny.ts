import { useAppSelector } from '~store/Store'
import { BehandlingFane } from '~components/behandling/sidemeny/IBehandlingInfo'

export function useSelectorBehandlingSidemenyFane(): BehandlingFane {
  return useAppSelector((state) => state.behandlingSidemenyReducer.fane)
}
