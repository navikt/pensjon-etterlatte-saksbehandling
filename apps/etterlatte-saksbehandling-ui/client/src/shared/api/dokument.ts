import {
  Journalpost,
  KnyttTilAnnenSakRequest,
  KnyttTilAnnenSakResponse,
  OppdaterJournalpostRequest,
} from '~shared/types/Journalpost'
import { apiClient, ApiResponse } from './apiClient'
import { ISak } from '~shared/types/sak'

export const hentDokumenter = async (fnr: string): Promise<ApiResponse<Journalpost[]>> =>
  apiClient.post(`/dokumenter`, { foedselsnummer: fnr })

// Midlertidig for å støtte uthenting av gjenlevendepensjon
export const hentAlleDokumenterInklPensjon = async (fnr: string): Promise<ApiResponse<Journalpost[]>> =>
  apiClient.post(`/dokumenter?visTemaPen=true`, { foedselsnummer: fnr })

export const ferdigstillJournalpost = async (args: { journalpostId: string; sak: ISak }): Promise<ApiResponse<Blob>> =>
  apiClient.post(`/dokumenter/${args.journalpostId}/ferdigstill`, { ...args.sak })

export const endreTemaJournalpost = async (args: {
  journalpostId: string
  nyttTema: string
}): Promise<ApiResponse<any>> => apiClient.put(`/dokumenter/${args.journalpostId}/tema/${args.nyttTema}`, {})

export const feilregistrerSakstilknytning = async (journalpostId: string): Promise<ApiResponse<any>> =>
  apiClient.put(`/dokumenter/${journalpostId}/feilregistrerSakstilknytning`, {})

export const opphevFeilregistrertSakstilknytning = async (journalpostId: string): Promise<ApiResponse<any>> =>
  apiClient.put(`/dokumenter/${journalpostId}/opphevFeilregistrertSakstilknytning`, {})

export const knyttTilAnnenSak = async (args: {
  journalpostId: string
  request: KnyttTilAnnenSakRequest
}): Promise<ApiResponse<KnyttTilAnnenSakResponse>> =>
  apiClient.put(`/dokumenter/${args.journalpostId}/knyttTilAnnenSak`, { ...args.request })

export const oppdaterJournalpost = async (args: {
  journalpost: OppdaterJournalpostRequest
  forsoekFerdigstill?: boolean
  journalfoerendeEnhet?: string
}): Promise<ApiResponse<any>> => {
  const queryParams = `journalfoerendeEnhet=${args.journalfoerendeEnhet}&forsoekFerdigstill=${args.forsoekFerdigstill}`

  return apiClient.put(`/dokumenter/${args.journalpost.journalpostId}?${queryParams}`, {
    ...args.journalpost,
  })
}

export const hentJournalpost = async (journalpostId: string): Promise<ApiResponse<Journalpost>> =>
  apiClient.get(`/dokumenter/${journalpostId}`)

export const hentDokumentPDF = async (args: {
  journalpostId: string
  dokumentInfoId: string
}): Promise<ApiResponse<Blob>> => apiClient.get(`/dokumenter/${args.journalpostId}/${args.dokumentInfoId}`)
