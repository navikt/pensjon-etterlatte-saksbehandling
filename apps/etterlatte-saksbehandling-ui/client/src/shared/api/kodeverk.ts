import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface Beskrivelse {
  term: string
  tekst: string
}

export interface Land {
  isoLandkode: string
  beskrivelse: Beskrivelse
}

export const hentKodeverkArkivtemaer = (): Promise<ApiResponse<Beskrivelse[]>> => apiClient.get('/kodeverk/arkivtemaer')
export const hentKodeverkLandISO2 = (): Promise<ApiResponse<Land[]>> => apiClient.get('/kodeverk/land-iso2')
