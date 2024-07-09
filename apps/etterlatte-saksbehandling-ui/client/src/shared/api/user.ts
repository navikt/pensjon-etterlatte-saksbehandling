import { InnloggetSaksbehandler } from '~shared/types/saksbehandler'
import { apiClient, ApiResponse } from './apiClient'

export const hentInnloggetSaksbehandler = async (): Promise<ApiResponse<InnloggetSaksbehandler>> => {
  return apiClient.get('/saksbehandlere/innlogget')
}

export const hentNavnforIdent = async (ident: string): Promise<ApiResponse<string>> => {
  return apiClient.get(`/saksbehandlere/navnforident/${ident}`)
}
