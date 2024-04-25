import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { AktivitetspliktOppfolging, IAktivitet, IOpprettAktivitet } from '~shared/types/Aktivitetsplikt'

export const hentAktivitetspliktOppfolging = async (args: {
  behandlingId: string
}): Promise<ApiResponse<AktivitetspliktOppfolging>> => apiClient.get(`/behandling/${args.behandlingId}/aktivitetsplikt`)

export interface OpprettAktivitetspliktOppfolgingRequest {
  aktivitet: string
}

export const opprettAktivitetspliktOppfolging = async (args: {
  behandlingId: string
  request: OpprettAktivitetspliktOppfolgingRequest
}): Promise<ApiResponse<AktivitetspliktOppfolging>> =>
  apiClient.post(`/behandling/${args.behandlingId}/aktivitetsplikt`, { ...args.request })

export const hentAktiviteter = async (args: { behandlingId: string }): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.get(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet`)

export const opprettAktivitet = async (args: {
  behandlingId: string
  request: IOpprettAktivitet
}): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.post(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet`, { ...args.request })
