import { ISaksbehandler } from '~store/reducers/SaksbehandlerReducer'
import { apiClient, ApiResponse } from './apiClient'

export const hentInnloggetSaksbehandler = async (): Promise<ApiResponse<ISaksbehandler>> => {
  return apiClient.get('/modiacontextholder/decorator')
}
