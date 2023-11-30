import { ISaksbehandler } from '~shared/types/saksbehandler'
import { apiClient, ApiResponse } from './apiClient'

export const hentInnloggetSaksbehandler = async (): Promise<ApiResponse<ISaksbehandler>> => {
  return apiClient.get('/innlogget')
}
