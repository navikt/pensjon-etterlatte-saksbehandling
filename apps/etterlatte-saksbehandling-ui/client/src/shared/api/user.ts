import { apiClient, ApiResponse } from './apiClient'

export const hentInnloggetSaksbehandler = async (): Promise<ApiResponse<any>> => {
  return apiClient.get('/modiacontextholder/decorator')
}
