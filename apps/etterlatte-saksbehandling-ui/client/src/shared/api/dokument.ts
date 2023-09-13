import { Journalpost } from '~shared/types/Journalpost'
import { apiClient, ApiResponse } from './apiClient'

export const hentDokumenter = async (fnr: string): Promise<ApiResponse<Journalpost[]>> =>
  apiClient.post(`/dokumenter`, { foedselsnummer: fnr })

export const ferdigstillJournalpost = async (journalpostId: string): Promise<ApiResponse<Blob>> =>
  apiClient.post(`/dokumenter/${journalpostId}/ferdigstill`, {})

export const hentDokumentPDF = async (args: {
  journalpostId: string
  dokumentInfoId: string
}): Promise<ApiResponse<Blob>> => apiClient.get(`/dokumenter/${args.journalpostId}/${args.dokumentInfoId}`)
