import { apiClient, ApiResponse } from './apiClient'

export const opprettEllerOppdaterBrevForVedtak = async (
  sakId: number,
  behandlingId: string
): Promise<ApiResponse<any>> => apiClient.post(`/brev/behandling/${behandlingId}/vedtak`, { sakId })

export const attesterVedtaksbrev = async (behandlingId: string): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/behandling/${behandlingId}/attestert`, {})

export const genererPdf = async (brevId: string): Promise<ApiResponse<ArrayBuffer>> =>
  apiClient.post(`/brev/${brevId}/pdf`, {})
