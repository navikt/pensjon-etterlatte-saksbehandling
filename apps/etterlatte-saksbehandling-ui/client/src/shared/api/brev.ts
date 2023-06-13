import { apiClient, ApiResponse } from './apiClient'
import { IBrev } from '~shared/types/Brev'

export const hentVedtaksbrev = async (behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/brev/behandling/${behandlingId}/vedtak`)

export const opprettVedtaksbrev = async (sakId: number, behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/behandling/${behandlingId}/vedtak?sakId=${sakId}`, {})

export const genererPdf = async (brevId: number, behandlingId: string): Promise<ApiResponse<ArrayBuffer>> =>
  apiClient.get(`/brev/behandling/${behandlingId}/vedtak/pdf?brevId=${brevId}`)

export const hentManuellPayload = async (props: { brevId: number; behandlingId: string }): Promise<ApiResponse<any>> =>
  apiClient.get(`/brev/behandling/${props.behandlingId}/vedtak/manuell?brevId=${props.brevId}`)

export const lagreManuellPayload = async (props: {
  brevId: number
  behandlingId: string
  payload: any
}): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/behandling/${props.behandlingId}/vedtak/manuell`, {
    id: props.brevId,
    payload: props.payload,
  })
