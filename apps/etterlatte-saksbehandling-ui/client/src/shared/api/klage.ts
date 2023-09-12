import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Formkrav, Klage } from '~shared/types/Klage'

export function opprettNyKlage(sakId: number): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/opprett/${sakId}`, {})
}

export function hentKlage(klageId: string): Promise<ApiResponse<Klage>> {
  return apiClient.get(`/klage/${klageId}`)
}

export function hentKlagerISak(sakId: number): Promise<ApiResponse<Array<Klage>>> {
  return apiClient.get(`/klage/sak/${sakId}`)
}

export function oppdaterFormkravIKlage(args: { klageId: string; formkrav: Formkrav }): Promise<ApiResponse<Klage>> {
  const { klageId, formkrav } = args
  return apiClient.put(`/klage/${klageId}/formkrav`, { formkrav })
}
