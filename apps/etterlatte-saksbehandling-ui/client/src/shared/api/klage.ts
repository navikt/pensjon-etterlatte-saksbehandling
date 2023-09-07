import { apiClient } from '~shared/api/apiClient'
import { Klage } from '~shared/types/Klage'

export function opprettNyKlage(sakId: number) {
  return apiClient.post<Klage>(`/klage/opprett/${sakId}`, {})
}
