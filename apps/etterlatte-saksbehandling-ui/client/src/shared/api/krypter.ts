import { apiClient, ApiError, ApiResponse } from '~shared/api/apiClient'
import { DekrypterResponse, KrypterResponse } from '~shared/types/Krypter'

export const krypter = async (dekryptert: string): Promise<ApiResponse<KrypterResponse>> =>
  apiClient.post(`/krypter/krypter`, { request: dekryptert })

export const dekrypter = async (kryptert: string): Promise<ApiResponse<DekrypterResponse>> =>
  apiClient.post(`/krypter/dekrypter`, { request: kryptert })

import { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapResult } from '~shared/api/apiUtils'

const Kryptering = ({ fnr }: { fnr: string | undefined | null }) => {
  const [kryptert, krypterFetch] = useApiCall(krypter)

  useEffect(() => {
    if (fnr) {
      krypterFetch(fnr)
    }
  }, [fnr])

  return mapResult(kryptert, {
    success: (res) => res.respons,
    error: (error: ApiError) => {
      console.log(error)
    },
  })
}

export default Kryptering
