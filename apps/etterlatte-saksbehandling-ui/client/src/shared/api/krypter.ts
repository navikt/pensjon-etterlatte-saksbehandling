import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { DekrypterResponse, KrypterResponse } from '~shared/types/Krypter'

export const krypter = async (dekryptert: string): Promise<ApiResponse<KrypterResponse>> =>
  apiClient.post(`/krypter/krypter`, { request: dekryptert })

export const dekrypter = async (kryptert: string): Promise<ApiResponse<DekrypterResponse>> =>
  apiClient.post(`/krypter/dekrypter`, { request: kryptert })

import { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapSuccess } from '~shared/api/apiUtils'

const krypterFnr = ({ fnr }: { fnr: string | undefined | null }) => {
  const [kryptert, krypterFetch] = useApiCall(krypter)

  useEffect(() => {
    if (fnr) {
      krypterFetch(fnr)
    }
  }, [fnr])

  return mapSuccess(kryptert, (res) => res.respons)
}

export default krypterFnr
