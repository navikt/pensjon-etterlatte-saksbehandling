import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface Beskrivelse {
  navn: string
  term: string
}

export const hentKodeverkArkivtemaer = (): Promise<ApiResponse<Beskrivelse[]>> => apiClient.get('/kodeverk/arkivtemaer')
