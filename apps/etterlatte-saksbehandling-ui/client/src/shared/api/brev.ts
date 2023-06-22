import { apiClient, ApiResponse } from './apiClient'
import { IBrev } from '~shared/types/Brev'

export const hentBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/brev/${props.brevId}?sakId=${props.sakId}`)

export const hentBrevForSak = async (sakId: number): Promise<ApiResponse<IBrev[]>> =>
  apiClient.get(`/brev/sak/${sakId}`)

export const opprettBrevForSak = async (sakId: number): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/sak/${sakId}`, {})

export const hentVedtaksbrev = async (behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/brev/behandling/${behandlingId}/vedtak`)

export const opprettVedtaksbrev = async (sakId: number, behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/behandling/${behandlingId}/vedtak?sakId=${sakId}`, {})

export const genererPdf = async (props: {
  brevId: number
  sakId?: number
  behandlingId?: string
}): Promise<ApiResponse<ArrayBuffer>> => {
  if (props.behandlingId) {
    return apiClient.get(`/brev/behandling/${props.behandlingId}/vedtak/pdf?brevId=${props.brevId}`)
  } else if (props.sakId && !props.behandlingId) {
    return apiClient.get(`/brev/${props.brevId}/pdf?sakId=${props.sakId}`)
  } else {
    throw Error('BehandlingId eller sakId må være satt!')
  }
}

export const hentManuellPayload = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.get(`/brev/${props.brevId}/payload?sakId=${props.sakId}`)

export const lagreManuellPayload = async (props: {
  brevId: number
  sakId: number
  payload: any
}): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/payload?sakId=${props.sakId}`, {
    id: props.brevId,
    payload: props.payload,
  })

export const ferdigstillBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/ferdigstill?sakId=${props.sakId}`, {})

export const journalfoerBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/journalfoer?sakId=${props.sakId}`, {})

export const distribuerBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/distribuer?sakId=${props.sakId}`, {})
