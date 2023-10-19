import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { IPdlPerson } from '~shared/types/Person'

export function hentGenerellBehandling(generellbehandlingId: string): Promise<ApiResponse<Generellbehandling>> {
  return apiClient.get(`/generellbehandling/hent/${generellbehandlingId}`)
}

export function oppdaterGenerellBehandling(generellbehandling: Generellbehandling): Promise<ApiResponse<void>> {
  return apiClient.put(`/generellbehandling/oppdater/${generellbehandling.sakId}`, { ...generellbehandling })
}

export function sendTilAttesteringGenerellBehandling(
  generellbehandling: Generellbehandling
): Promise<ApiResponse<void>> {
  return apiClient.put(`/generellbehandling/sendtilattestering/${generellbehandling.sakId}`, { ...generellbehandling })
}

export function attesterGenerellbehandling(generellbehandling: Generellbehandling): Promise<ApiResponse<void>> {
  return apiClient.post(`/generellbehandling/attester/${generellbehandling.sakId}/${generellbehandling.id}`, {})
}

export function hentKravpakkeforSak(sakId: number): Promise<ApiResponse<KravPakkeMedAvdoed>> {
  return apiClient.get(`/generellbehandling/kravpakkeForSak/${sakId}`)
}

export interface KravPakkeMedAvdoed {
  kravpakke: Generellbehandling & { innhold: KravpakkeUtland }
  avdoed: IPdlPerson
}
