import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { createContext } from 'react'

export const beregningConfig = {
  brukBehandling: false,
}

export const hentClientConfig = async (): Promise<ApiResponse<{ [key: string]: string }>> => apiClient.get('/config')

export const ConfigContext = createContext<{ [key: string]: string }>({})
