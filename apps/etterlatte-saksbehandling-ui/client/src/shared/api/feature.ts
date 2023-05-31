import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IFeature } from '~shared/types/IFeature'

export const hentFunksjonsbrytere = async (brytere: string[]): Promise<ApiResponse<IFeature[]>> => {
  return apiClient.post(`/feature`, { features: brytere })
}
