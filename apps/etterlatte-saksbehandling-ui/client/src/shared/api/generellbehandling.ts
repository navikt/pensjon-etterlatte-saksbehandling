import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Generellbehandling, GenerellBehandlingType, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { IPdlPerson } from '~shared/types/Person'

export function hentGenerelleBehandlingForSak(sakId: number): Promise<ApiResponse<Generellbehandling[]>> {
  return apiClient.get(`/generellbehandling/hentforsak/${sakId}`)
}

export function hentGenerellBehandling(generellbehandlingId: string): Promise<ApiResponse<Generellbehandling>> {
  return apiClient.get(`/generellbehandling/hent/${generellbehandlingId}`)
}

export function oppdaterGenerellBehandling(generellbehandling: Generellbehandling): Promise<ApiResponse<void>> {
  return apiClient.put(`/generellbehandling/oppdater/${generellbehandling.sakId}`, { ...generellbehandling })
}

export function avbrytGenerellBehandling(generellbehandling: Generellbehandling): Promise<ApiResponse<void>> {
  return apiClient.put(`/generellbehandling/avbryt/${generellbehandling.sakId}/${generellbehandling.id}`, {})
}

export function sendTilAttesteringGenerellBehandling(
  generellbehandling: Generellbehandling
): Promise<ApiResponse<void>> {
  return apiClient.put(`/generellbehandling/sendtilattestering/${generellbehandling.sakId}`, { ...generellbehandling })
}

export function opprettGenerellBehandling(args: {
  sakId: number
  type: GenerellBehandlingType
}): Promise<ApiResponse<Generellbehandling>> {
  return apiClient.post(`/generellbehandling/${args.sakId}`, { type: args.type })
}

export function attesterGenerellbehandling(generellbehandling: Generellbehandling): Promise<ApiResponse<void>> {
  return apiClient.post(`/generellbehandling/attester/${generellbehandling.sakId}/${generellbehandling.id}`, {})
}

export function underkjennGenerellbehandling({
  generellbehandling,
  begrunnelse,
}: {
  generellbehandling: Generellbehandling
  begrunnelse: string
}): Promise<ApiResponse<void>> {
  return apiClient.post(`/generellbehandling/underkjenn/${generellbehandling.sakId}/${generellbehandling.id}`, {
    begrunnelse: begrunnelse,
  })
}

export function hentKravpakkeforSak(sakId: number): Promise<ApiResponse<KravPakkeMedAvdoed>> {
  return apiClient.get(`/generellbehandling/kravpakkeForSak/${sakId}`)
}

export interface KravPakkeMedAvdoed {
  kravpakke: Generellbehandling & { innhold?: KravpakkeUtland | undefined }
  avdoed: IPdlPerson
}
