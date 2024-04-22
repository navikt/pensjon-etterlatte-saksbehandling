import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { ISanksjon, ISanksjonLagre } from '~components/behandling/sanksjon/Sanksjon'

export const hentSanksjon = async (behandlingId: string): Promise<ApiResponse<ISanksjon[]>> => {
  return apiClient.get(`/beregning/sanksjon/${behandlingId}`)
}

export const lagreSanksjon = async (args: {
  behandlingId: string
  sanksjon: ISanksjonLagre
}): Promise<ApiResponse<void>> => apiClient.post(`/beregning/sanksjon/${args.behandlingId}`, { ...args.sanksjon })

export const slettSanksjon = async (args: { behandlingId: string; sanksjonId: string }): Promise<ApiResponse<void>> =>
  apiClient.delete(`/beregning/sanksjon/${args.behandlingId}/${args.sanksjonId}`)
