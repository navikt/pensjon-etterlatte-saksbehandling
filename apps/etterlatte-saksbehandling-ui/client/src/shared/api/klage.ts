import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  AvsluttKlageRequest,
  Formkrav,
  IniteltUtfallMedBegrunnelseDto,
  Klage,
  KlageUtfallUtenBrev,
  NyKlageRequest,
} from '~shared/types/Klage'

export function opprettNyKlage(nyKlageRequest: NyKlageRequest): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/opprett/${nyKlageRequest.sakId}`, { ...nyKlageRequest })
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

export function oppdaterUtfallForKlage(args: {
  klageId: string
  utfall: KlageUtfallUtenBrev
}): Promise<ApiResponse<Klage>> {
  const { klageId, utfall } = args
  return apiClient.put(`/klage/${klageId}/utfall`, { utfall })
}

export function oppdaterInitieltUtfallForKlage(args: {
  klageId: string
  utfallMedBegrunnelse: IniteltUtfallMedBegrunnelseDto //TODO type her Må også slenges på klage obj dto
}): Promise<ApiResponse<Klage>> {
  const { klageId, utfallMedBegrunnelse } = args
  return apiClient.put(`/klage/${klageId}/initieltutfall`, { utfallMedBegrunnelse })
}

export function ferdigstillKlagebehandling(klageId: string): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/${klageId}/ferdigstill`, {})
}

export function avsluttKlage(avsluttKlageRequest: AvsluttKlageRequest): Promise<ApiResponse<void>> {
  return apiClient.post(`/klage/${avsluttKlageRequest.klageId}/avbryt`, { ...avsluttKlageRequest })
}
