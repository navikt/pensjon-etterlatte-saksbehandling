import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IFeature } from '~shared/types/IFeature'
import { logger } from '~utils/logger'

export const hentFunksjonsbrytere = async (brytere: string[]): Promise<ApiResponse<IFeature[]>> => {
  function post(): Promise<ApiResponse<IFeature[]>> {
    return apiClient.post(`/feature`, { features: brytere })
  }

  try {
    return post()
  } catch (e) {
    logger.generalError({ message: 'Feil i henting av brytere mot Unleash. Prøver på nytt' })
    return post()
  }
}
