import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { ISak, SakType } from '~shared/types/sak'
import { SakMedBehandlinger } from '~components/person/typer'

export interface Navkontor {
  navn: string
}

export const hentNavkontorForPerson = async (fnr: string): Promise<ApiResponse<Navkontor>> => {
  return apiClient.post(`/personer/navkontor`, { foedselsnummer: fnr })
}

export const hentSakMedBehandlnger = async (fnr: string): Promise<ApiResponse<SakMedBehandlinger>> => {
  return apiClient.post(`/personer/behandlingerforsak`, { foedselsnummer: fnr })
}

export const hentSak = async (sakId: number): Promise<ApiResponse<ISak>> => {
  return apiClient.get(`sak/${sakId}`)
}

export const hentSakForPerson = async (args: { fnr: string; type: SakType }): Promise<ApiResponse<ISak>> =>
  apiClient.post(`/personer/sak/${args.type}`, { foedselsnummer: args.fnr })

export const byttEnhetPaaSak = async (args: { sakId: number; enhet: String }): Promise<ApiResponse<void>> => {
  return apiClient.post(`sak/${args.sakId}/endre_enhet`, { enhet: args.enhet })
}
