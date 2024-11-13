import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface Beskrivelse {
  term: string
  tekst: string
}

export interface KodeverkLand {
  isoLandkode: string
  beskrivelse: Beskrivelse
}

export const hentKodeverkArkivtemaer = (): Promise<ApiResponse<Beskrivelse[]>> => apiClient.get('/kodeverk/arkivtemaer')
export const hentKodeverkLandISO2 = (): Promise<ApiResponse<KodeverkLand[]>> => apiClient.get('/kodeverk/land-iso2')
