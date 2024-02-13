import { SaksbehandlerMedInformasjon } from '~shared/types/saksbehandler'
import { apiClient, ApiResponse } from './apiClient'

export const hentInnloggetSaksbehandler = async (): Promise<ApiResponse<SaksbehandlerMedInformasjon>> => {
  return apiClient.get('/saksbehandlere/innlogget')
}
