import { Journalpost } from '~components/behandling/types'
import { apiClient, ApiResponse } from './apiClient'

export const hentDokumenter = async (fnr: string): Promise<ApiResponse<Journalpost[]>> =>
  apiClient.post(`/dokumenter`, { foedselsnummer: fnr })

export const hentDokumentPDF = async (journalpostId: string, dokumentInfoId: string): Promise<ApiResponse<Blob>> =>
  apiClient.get(`/dokumenter/${journalpostId}/${dokumentInfoId}`)
