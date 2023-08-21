import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { createContext } from 'react'

export const hentClientConfig = async (): Promise<ApiResponse<Map<string, string>>> => apiClient.get('/config')

export const ConfigContext = createContext<{ [key: string]: string }>({})
