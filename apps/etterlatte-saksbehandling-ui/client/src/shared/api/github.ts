import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface Release {
  id: number
  name: string
  published_at: string
  body: string
}

export const hentUtgivelser = (): Promise<ApiResponse<Release[]>> => apiClient.get('/github/releases')
