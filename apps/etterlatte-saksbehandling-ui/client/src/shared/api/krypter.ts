import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { DekrypterResponse, KrypterResponse } from '~shared/types/Krypter'

export const krypter = async (dekryptert: string): Promise<ApiResponse<KrypterResponse>> =>
  apiClient.post(`/krypter`, { dekryptert })

export const dekrypter = async (kryptert: string): Promise<ApiResponse<DekrypterResponse>> =>
  apiClient.post(`/dekrypter`, { kryptert })
