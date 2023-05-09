import { apiClient, ApiResponse } from './apiClient'

export const hentVedtaksbrev = async (behandlingId: string): Promise<ApiResponse<any>> =>
  apiClient.get(`/brev/behandling/${behandlingId}/vedtak`)

export const opprettVedtaksbrev = async (sakId: number, behandlingId: string): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/behandling/${behandlingId}/vedtak?sakId=${sakId}`, {})

export const genererPdf = async (sakId: number, behandlingId: string): Promise<ApiResponse<ArrayBuffer>> =>
  apiClient.get(`/brev/behandling/${behandlingId}/vedtak/pdf?sakId=${sakId}`)
