import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { PersonMedNavn } from '~shared/types/grunnlag'

export const hentPersonerISak = async (sakId: number): Promise<ApiResponse<PersonerISakResponse>> => {
  return apiClient.get(`/grunnlag/${sakId}/personer/alle`)
}

export type PersonerISakResponse = {
  personer: Record<string, PersonMedNavn>
}
