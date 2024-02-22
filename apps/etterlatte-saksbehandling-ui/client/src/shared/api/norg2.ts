import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface RsKode {
  navn: string
  term: string
}

export const hentKodeverkTema = (): Promise<ApiResponse<RsKode[]>> => apiClient.get('/norg2/kodeverk/tema')
