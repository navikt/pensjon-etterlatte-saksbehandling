import { apiClient, ApiResponse } from '~shared/api/apiClient'

export const hentUrlForInntektOversikt = (fnr: string): Promise<ApiResponse<{ url: string | undefined }>> => {
  return apiClient.post('/arbeid-og-inntekt/url-for-inntekt-oversikt', { fnr })
}
