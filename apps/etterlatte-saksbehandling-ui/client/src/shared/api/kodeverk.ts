import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface Beskrivelse {
  term: string
  tekst: string
}

export const hentKodeverkArkivtemaer = (): Promise<ApiResponse<Beskrivelse[]>> => apiClient.get('/kodeverk/arkivtemaer')
