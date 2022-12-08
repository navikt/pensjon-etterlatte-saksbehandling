import { apiClient, ApiResponse } from './apiClient'

export const hentMaler = async (): Promise<ApiResponse<Mal[]>> => apiClient.get(`/brev/maler`)

export const hentMottakere = async (): Promise<ApiResponse<any>> => apiClient.get(`/brev/mottakere`)

export const hentBrevForBehandling = async (behandlingId: string): Promise<ApiResponse<any>> =>
  apiClient.get(`/brev/behandling/${behandlingId}`)

export const nyttBrevForBehandling = async (
  behandlingId: string,
  mottaker: Mottaker,
  mal: any,
  enhet: string
): Promise<ApiResponse<any>> => apiClient.post(`/brev/behandling/${behandlingId}`, { mottaker, mal, enhet })

export const opprettBrevFraPDF = async (behandlingId: string, mottaker: Mottaker, pdf: FormData): Promise<any> => {
  return await fetch(`/brev/pdf/${behandlingId}`, {
    method: 'POST',
    body: pdf,
  }).then((res) => {
    if (res.status == 200) {
      return res.json()
    } else {
      throw Error(res.statusText)
    }
  })
}

export const opprettEllerOppdaterBrevForVedtak = async (sakId: number, behandlingId: string): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/vedtak`, { sakId, behandlingId })

export const ferdigstillBrev = async (brevId: string): Promise<ApiResponse<any>> =>
  apiClient.post(`/brev/${brevId}/ferdigstill`, {})

export const slettBrev = async (brevId: string): Promise<ApiResponse<any>> => apiClient.delete(`/brev/${brevId}`)

export const genererPdf = async (brevId: string): Promise<ApiResponse<ArrayBuffer>> =>
  apiClient.post(`/brev/${brevId}/pdf`, {})

export const hentForhaandsvisning = async (
  mottaker: Mottaker,
  mal: any,
  enhet: string
): Promise<ApiResponse<ArrayBuffer>> => apiClient.post(`/brev/forhaandsvisning`, { mottaker, mal, enhet })

export interface Mottaker {
  foedselsnummer?: string
  orgnummer?: string
  adresse?: Adresse
}

export interface Adresse {
  fornavn?: string
  etternavn?: string
  adresse?: string
  postnummer?: string
  poststed?: string
}

export interface Mal {
  tittel: string
  navn: string
}
