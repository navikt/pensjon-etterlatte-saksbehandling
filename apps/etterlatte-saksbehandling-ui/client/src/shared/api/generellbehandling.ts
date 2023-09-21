import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Generellbehandling, GenerellBehandlingType } from '~shared/types/Generellbehandling'

export function opprettNyGenerellBehandling(args: {
  sakId: number
  type: GenerellBehandlingType
}): Promise<ApiResponse<Generellbehandling>> {
  const { type, sakId } = args
  return apiClient.post(`/generellbehandling/${sakId}`, { type })
}

export function hentGenerellBehandling(generellbehandlingId: string): Promise<ApiResponse<Generellbehandling>> {
  return apiClient.get(`/generellbehandling/${generellbehandlingId}`)
}

export function oppdaterGenerellBehandling(generellbehandling: Generellbehandling): Promise<ApiResponse<void>> {
  return apiClient.put(`/generellbehandling/${generellbehandling.id}`, { generellbehandling })
}
