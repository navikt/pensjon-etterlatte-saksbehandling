import { Journalpost } from '~shared/types/Journalpost'
import { apiClient, ApiResponse } from './apiClient'
import { ISak } from '~shared/types/sak'

export const hentDokumenter = async (fnr: string): Promise<ApiResponse<Journalpost[]>> =>
  apiClient.post(`/dokumenter`, { foedselsnummer: fnr })

export const ferdigstillJournalpost = async (args: { journalpostId: string; sak: ISak }): Promise<ApiResponse<Blob>> =>
  apiClient.post(`/dokumenter/${args.journalpostId}/ferdigstill`, { ...args.sak })

export const endreTemaJournalpost = async (args: {
  journalpostId: string
  nyttTema: string
}): Promise<ApiResponse<any>> => apiClient.put(`/dokumenter/${args.journalpostId}/tema/${args.nyttTema}`, {})

export const hentJournalpost = async (journalpostId: string): Promise<ApiResponse<Journalpost>> =>
  apiClient.get(`/dokumenter/${journalpostId}`)

export const hentDokumentPDF = async (args: {
  journalpostId: string
  dokumentInfoId: string
}): Promise<ApiResponse<Blob>> => apiClient.get(`/dokumenter/${args.journalpostId}/${args.dokumentInfoId}`)
