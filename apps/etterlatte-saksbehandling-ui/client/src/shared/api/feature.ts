import {apiClient, ApiError, ApiResponse } from '~shared/api/apiClient'
import { IFeature } from '~shared/types/IFeature'
import { logger } from '~utils/logger'

export const hentFunksjonsbrytere = async (brytere: string[]): Promise<ApiResponse<IFeature[]>> => {
  function post(): Promise<ApiResponse<IFeature[]>> {
    return apiClient.post(`/feature`, { features: brytere })
  }

  const promise = () => post();
  return retry(promise)
}

async function retry(promise: () => Promise<ApiResponse<IFeature[]>>) {
  return retryInner(2, promise)
}

async function retryInner(times: number, promise: () => Promise<ApiResponse<IFeature[]>>) {
  if (times < 1) {
    return { ok: false } as ApiError
  }
  const res = await promise();
  if (res.ok) {
    return res
  } else {
    logger.generalError({message: 'Feil i henting av brytere mot Unleash. Prøver på nytt'})
    return retryInner(times -1, promise)
  }
}
