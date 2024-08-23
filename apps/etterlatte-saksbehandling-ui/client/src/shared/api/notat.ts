import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface Notat {
  id: number
  sakId: number
  journalpostId: string
  tittel: string
  opprettet: string
}

export enum NotatMal {
  TOM_MAL = 'TOM_MAL',
}

export function opprettNotatForSak(args: {
  sakId: number
  referanse?: string
  mal: string
}): Promise<ApiResponse<Notat>> {
  const params = new URLSearchParams({
    mal: args.mal,
  })
  if (args.referanse) params.append('referanse', args.referanse)

  return apiClient.post(`/notat/sak/${args.sakId}?${params}`, {})
}

export function hentNotaterForSak(sakId: number): Promise<ApiResponse<Notat[]>> {
  return apiClient.get(`/notat/sak/${sakId}`)
}

export function hentNotaterForReferanse(referanse: string): Promise<ApiResponse<Notat[]>> {
  return apiClient.get(`/notat/referanse/${referanse}`)
}

export function slettNotat(id: number): Promise<ApiResponse<Notat>> {
  return apiClient.delete(`/notat/${id}`)
}

export function hentNotatPayload(id: number): Promise<ApiResponse<any>> {
  return apiClient.get(`/notat/${id}/payload`)
}

export function lagreNotatPayload(args: { id: number; payload: any }): Promise<ApiResponse<any>> {
  return apiClient.post(`/notat/${args.id}/payload`, args.payload)
}

export function oppdaterNotatTittel(args: { id: number; tittel: string }): Promise<ApiResponse<any>> {
  return apiClient.post(`/notat/${args.id}/tittel?tittel=${args.tittel}`, {})
}

export function genererNotatPdf(id: number): Promise<ApiResponse<ArrayBuffer>> {
  return apiClient.get(`/notat/${id}/pdf`)
}

export function journalfoerNotat(id: number): Promise<ApiResponse<ArrayBuffer>> {
  return apiClient.post(`/notat/${id}/journalfoer`, {})
}
