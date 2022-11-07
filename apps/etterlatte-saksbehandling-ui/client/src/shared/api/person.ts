import { apiClient, ApiResponse } from './apiClient'

export const getPerson = async (fnr: string): Promise<ApiResponse<any>> => {
  return apiClient.get(`/personer/${fnr}`)
}
