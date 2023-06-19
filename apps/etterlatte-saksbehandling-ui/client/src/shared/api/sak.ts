import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const hentSisteIverksatteBehandling = async (sakId: number): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.get(`/saker/${sakId}/behandlinger/sisteIverksatte`)
}
