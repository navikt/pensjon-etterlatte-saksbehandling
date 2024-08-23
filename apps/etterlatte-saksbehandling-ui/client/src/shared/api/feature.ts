import { apiClient, ApiError, ApiResponse } from '~shared/api/apiClient'
import { IFeature } from '~shared/types/IFeature'
import { logger } from '~utils/logger'

export const hentFunksjonsbrytere = async (brytere: string[]): Promise<ApiResponse<IFeature[]>> => {
  function post(): Promise<ApiResponse<IFeature[]>> {
    return apiClient.post(`/feature`, { features: brytere })
  }

  const promise = () => post()
  return retry(promise)
}

async function retry(promise: () => Promise<ApiResponse<IFeature[]>>) {
  const numTries = 2
  return retryInner(numTries, promise)
}

async function retryInner(
  times: number,
  promise: () => Promise<ApiResponse<IFeature[]>>
): Promise<ApiResponse<IFeature[]> | ApiError> {
  if (times < 1) {
    return { ok: false } as ApiError
  }

  return promise()
    .then((res) => {
      if (res.ok) {
        return res
      } else {
        logger.generalError({ message: 'Feil i henting av brytere mot Unleash. Prøver på nytt' })
        return retryInner(times - 1, promise)
      }
    })
    .catch((err) => {
      logger.generalError({ message: `Feil i henting av brytere mot Unleash. Prøver på nytt... error: ${err}` })
      return retryInner(times - 1, promise)
    })
}
