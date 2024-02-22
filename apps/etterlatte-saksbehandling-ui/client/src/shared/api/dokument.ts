import {
  Journalpost,
  JournalpostUtsendingsinfo,
  KnyttTilAnnenSakRequest,
  KnyttTilAnnenSakResponse,
  OppdaterJournalpostRequest,
} from '~shared/types/Journalpost'
import { apiClient, ApiResponse } from './apiClient'

export const hentDokumenter = async (fnr: string): Promise<ApiResponse<Journalpost[]>> =>
  apiClient.post(`/dokumenter`, { foedselsnummer: fnr })

// Midlertidig for å støtte uthenting av gjenlevendepensjon
export const hentAlleDokumenterInklPensjon = async (fnr: string): Promise<ApiResponse<Journalpost[]>> =>
  apiClient.post(`/dokumenter?visTemaPen=true`, { foedselsnummer: fnr })

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
  const queryParams = args.forsoekFerdigstill
    ? `journalfoerendeEnhet=${args.journalfoerendeEnhet}&forsoekFerdigstill=${args.forsoekFerdigstill}`
    : ''

  return apiClient.put(`/dokumenter/${args.journalpost.journalpostId}?${queryParams}`, {
    ...args.journalpost,
  })
}

export const hentJournalpost = async (journalpostId: string): Promise<ApiResponse<Journalpost>> =>
  apiClient.get(`/dokumenter/${journalpostId}`)

export const hentUtsendingsinfo = async (args: {
  journalpostId: string
}): Promise<ApiResponse<JournalpostUtsendingsinfo>> => apiClient.get(`/dokumenter/${args.journalpostId}/utsendingsinfo`)

export const hentDokumentPDF = async (args: {
  journalpostId: string
  dokumentInfoId: string
}): Promise<ApiResponse<Blob>> => apiClient.get(`/dokumenter/${args.journalpostId}/${args.dokumentInfoId}`)
