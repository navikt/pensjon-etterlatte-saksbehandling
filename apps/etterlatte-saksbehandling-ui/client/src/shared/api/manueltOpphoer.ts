import { apiClient, ApiResponse } from './apiClient'
import { Opphoersgrunn } from '../../components/person/ManueltOpphoerModal'

export type ManueltOpphoerResponse = {
  behandlingId: string
}

export async function sendInnManueltOpphoer(
  sakId: number,
  opphoerAarsaker: Opphoersgrunn[],
  fritekstAarsak: string
): Promise<ApiResponse<ManueltOpphoerResponse>> {
  return await apiClient.post<ManueltOpphoerResponse>(`/saker/${sakId}/manueltopphoer`, {
    sak: sakId,
    fritekstAarsak,
    opphoerAarsaker,
  })
}
