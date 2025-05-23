import { apiClient, ApiResponse } from './apiClient'
import { Brevtype, IBrev, Mottaker, Spraak } from '~shared/types/Brev'
import { DokDistKanalResponse } from '~shared/types/dokdist'

export const hentBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/brev/${props.brevId}?sakId=${props.sakId}`)

export const hentBrevForSak = async (sakId: number): Promise<ApiResponse<IBrev[]>> =>
  apiClient.get(`/brev/sak/${sakId}`)

export const opprettBrevForSak = async (sakId: number): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/sak/${sakId}`, {})

export const opprettBrevAvSpesifikkTypeForSak = async (props: {
  sakId: number
  body: Record<string, unknown>
}): Promise<ApiResponse<IBrev>> => apiClient.post(`/brev/sak/${props.sakId}/spesifikk`, props.body)

export const hentVarselbrev = async (behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/brev/behandling/${behandlingId}/varsel`)

export const opprettVarselbrev = async (args: { sakId: number; behandlingId: string }): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/behandling/${args.behandlingId}/varsel?sakId=${args.sakId}`, {})

export const hentBrevTilBehandling = async (behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.get(`/behandling/brev/${behandlingId}`)

export const ferdigstillBrevTilBehandling = async (behandlingId: string): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/behandling/brev/${behandlingId}/ferdigstill`, {})

export const opprettBrevTilBehandling = async (args: {
  sakId: number
  behandlingId: string
}): Promise<ApiResponse<IBrev>> => {
  return apiClient.post(`/behandling/brev/${args.behandlingId}?sakId=${args.sakId}`, {})
}

export const opprettMottaker = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<Mottaker>> =>
  apiClient.post(`/brev/${props.brevId}/mottaker?sakId=${props.sakId}`, {})

export const tilbakestillMottakere = async (props: {
  brevId: number
  sakId: number
}): Promise<ApiResponse<Mottaker[]>> =>
  apiClient.post(`/brev/${props.brevId}/mottaker/tilbakestill?sakId=${props.sakId}`, {})

export const oppdaterMottaker = async (props: {
  brevId: number
  sakId: number
  mottaker: Mottaker
}): Promise<ApiResponse<void>> =>
  apiClient.put(`/brev/${props.brevId}/mottaker?sakId=${props.sakId}`, { mottaker: props.mottaker })

export const settHovedmottaker = async (props: {
  brevId: number
  sakId: number
  mottakerId: string
}): Promise<ApiResponse<void>> =>
  apiClient.put(`/brev/${props.brevId}/mottaker/${props.mottakerId}/hoved?sakId=${props.sakId}`, {})

export const slettMottaker = async (props: {
  brevId: number
  mottakerId: string
  sakId: number
}): Promise<ApiResponse<void>> =>
  apiClient.delete(`/brev/${props.brevId}/mottaker/${props.mottakerId}?sakId=${props.sakId}`)

export const oppdaterTittel = async (args: {
  brevId: number
  sakId: number
  tittel: string
}): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/${args.brevId}/tittel?sakId=${args.sakId}`, { tittel: args.tittel })

export const oppdaterSpraak = async (args: {
  brevId: number
  sakId: number
  spraak: Spraak
}): Promise<ApiResponse<IBrev>> =>
  apiClient.post(`/brev/${args.brevId}/spraak?sakId=${args.sakId}`, { spraak: args.spraak })

export const slettBrev = async (args: { brevId: number; sakId: number }): Promise<ApiResponse<IBrev>> =>
  apiClient.delete(`/brev/${args.brevId}?sakId=${args.sakId}`)

export const genererPdf = async (props: {
  brevId: number
  sakId?: number
  behandlingId?: string
  brevtype: Brevtype
  skalGaaViaBehandling?: boolean
}): Promise<ApiResponse<ArrayBuffer>> => {
  if (props.brevtype === Brevtype.VEDTAK || props.skalGaaViaBehandling) {
    return apiClient.get(`/behandling/brev/${props.behandlingId}/pdf?brevId=${props.brevId}&sakId=${props.sakId}`)
  } else if (props.brevtype === Brevtype.VARSEL) {
    return apiClient.get(`/brev/behandling/${props.behandlingId}/varsel/pdf?brevId=${props.brevId}`)
  } else if (props.brevtype === Brevtype.OVERSENDELSE_KLAGE) {
    return apiClient.get(`/brev/behandling/${props.behandlingId}/oversendelse/pdf?brevId=${props.brevId}`)
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
  brevtype: Brevtype
}): Promise<ApiResponse<any>> => {
  return apiClient.put(
    `/behandling/brev/${props.behandlingId}/tilbakestill?brevId=${props.brevId}&sakId=${props.sakId}&brevtype=${props.brevtype}`,
    {}
  )
}

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

export const ferdigstillBrev = async (props: {
  brevId: number
  sakId: number
  brevtype: Brevtype
}): Promise<ApiResponse<any>> => {
  if (props.brevtype === Brevtype.VARSEL) {
    return apiClient.post(`/brev/${props.brevId}/varsel/ferdigstill?sakId=${props.sakId}`, {})
  } else {
    return apiClient.post(`/brev/${props.brevId}/ferdigstill?sakId=${props.sakId}`, {})
  }
}

export const journalfoerBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/journalfoer?sakId=${props.sakId}`, {})

export const distribuerBrev = async (props: { brevId: number; sakId: number }): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/distribuer?sakId=${props.sakId}`, {})

export const markerBrevSomUtgaar = async (props: {
  brevId: number
  sakId: number
  kommentar: string
}): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${props.brevId}/utgaar?sakId=${props.sakId}`, { kommentar: props.kommentar })

export const hentDistKanal = async (args: {
  brevId: number
  sakId: number
  mottakerId: string
}): Promise<ApiResponse<DokDistKanalResponse>> =>
  apiClient.get(`/brev/${args.brevId}/kanal?sakId=${args.sakId}&mottakerId=${args.mottakerId}`)
