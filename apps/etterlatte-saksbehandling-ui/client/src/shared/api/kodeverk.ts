import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface RsKode {
  navn: string
  term: string
}

export const hentKodeverkArkivtemaer = (): Promise<ApiResponse<RsKode[]>> => apiClient.get('/kodeverk/Arkivtemaer')
