import { apiClient, ApiResponse } from './apiClient'
import { IBrev, Mottaker } from '~shared/types/Brev'

import { isFailureWithCode, isSuccess, Result } from '~shared/api/apiUtils'

export const hentBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/brev/${props.brevId}?sakId=${props.sakId}`)

export const hentBrevForSak = async (sakId: number): Promise<ApiResponse<IBrev[]>> =>
  apiClient.get(`/brev/sak/${sakId}`)

export const opprettBrevForSak = async (sakId: number): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/sak/${sakId}`, {})

export const hentVedtaksbrev = async (behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/brev/behandling/${behandlingId}/vedtak`)

export const ferdigstillVedtaksbrev = async (behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/behandling/${behandlingId}/vedtak/ferdigstill`, {})

export const opprettVedtaksbrev = async (args: { sakId: number; behandlingId: string }): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/behandling/${args.behandlingId}/vedtak?sakId=${args.sakId}`, {})

export const oppdaterMottaker = async (props: {
  brevId: number
  sakId: number
  mottaker: Mottaker
}): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/${props.brevId}/mottaker?sakId=${props.sakId}`, { mottaker: props.mottaker })

export const oppdaterTittel = async (args: {
  brevId: number
  sakId: number
  tittel: string
}): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/${args.brevId}/tittel?sakId=${args.sakId}`, { tittel: args.tittel })

export const slettBrev = async (args: { brevId: number; sakId: number }): Promise<ApiResponse<IBrev>> =>
  apiClient.delete(`/brev/${args.brevId}?sakId=${args.sakId}`)

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

export const opprettBrevFraPDF = async (args: { sakId: number; formData: FormData }): Promise<ApiResponse<IBrev>> => {
  return apiClient.postFormData(`/brev/sak/${args.sakId}/pdf`, args.formData)
}

export const hentManuellPayload = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.get(`/brev/${props.brevId}/payload?sakId=${props.sakId}`)

export const tilbakestillManuellPayload = async (props: {
  brevId: number
  sakId: number
  behandlingId: string
}): Promise<ApiResponse<any>> =>
  apiClient.put(`/brev/behandling/${props.behandlingId}/payload/tilbakestill`, {
    brevId: props.brevId,
    sakId: props.sakId,
  })

export const lagreManuellPayload = async (props: {
  brevId: number
  sakId: number
  payload: any
  payload_vedlegg: any
}): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/payload?sakId=${props.sakId}`, {
    id: props.brevId,
    payload: props.payload,
    payload_vedlegg: props.payload_vedlegg,
  })

export const ferdigstillBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/ferdigstill?sakId=${props.sakId}`, {})

export const journalfoerBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/journalfoer?sakId=${props.sakId}`, {})

export const distribuerBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/distribuer?sakId=${props.sakId}`, {})

export const isSuccessOrNotFound = (result: Result<any>) => {
  return isSuccess(result) || isFailureWithCode(result, 404)
}
export const getData = <T>(result: Result<T>) => {
  return isSuccess(result) ? result.data : undefined
}
