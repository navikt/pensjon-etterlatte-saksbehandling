import { IDetaljertBehandling, Virkningstidspunkt, IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { apiClient, ApiResponse } from './apiClient'

export const hentBehandlingerForPerson = async (fnr: string): Promise<ApiResponse<any>> => {
  return apiClient.get(`/personer/${fnr}/behandlinger`)
}

export const hentGrunnlagsendringshendelserForPerson = async (fnr: string): Promise<ApiResponse<any>> => {
  return apiClient.get(`/personer/${fnr}/grunnlagsendringshendelser`)
}

export const hentBehandling = async (id: string): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.get(`/behandling/${id}`)
}

export const avbrytBehandling = async (id: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/behandling/${id}/avbryt`, {})
}

export const fastsettVirkningstidspunkt = async (args: {
  id: string
  dato: Date
}): Promise<ApiResponse<Virkningstidspunkt>> => {
  return apiClient.post(`/behandling/${args.id}/virkningstidspunkt`, { dato: args.dato })
}

export const fattVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/fattvedtak/${behandlingsId}`, {})
}

export const upsertVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/upsert/${behandlingsId}`, {})
}

export const attesterVedtak = async (behandlingId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/attester/${behandlingId}`, {})
}

export const underkjennVedtak = async (
  behandlingId: string,
  kommentar: string,
  valgtBegrunnelse: string
): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/underkjenn/${behandlingId}`, { kommentar, valgtBegrunnelse })
}

export const lagreBegrunnelseKommerBarnetTilgode = async (args: {
  behandlingId: string
  begrunnelse: string
  svar: string
}): Promise<ApiResponse<IKommerBarnetTilgode>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/kommerbarnettilgode`, {
    svar: args.svar,
    begrunnelse: args.begrunnelse,
  })
}

export const lagreSoeskenMedIBeregning = async (
  behandlingsId: string,
  soeskenMedIBeregning: { foedselsnummer: string; skalBrukes: boolean }[]
): Promise<ApiResponse<any>> => {
  return apiClient.post(`/grunnlag/beregningsgrunnlag/${behandlingsId}`, { soeskenMedIBeregning })
}

interface GrunnlagResponse {
  response: string
}

export const slettPeriodeForAvdoedesMedlemskap = async (
  behandlingsId: string,
  saksbehandlerPeriodeId: string
): Promise<ApiResponse<GrunnlagResponse>> =>
  apiClient.delete<GrunnlagResponse>(`/grunnlag/saksbehandler/periode/${behandlingsId}/${saksbehandlerPeriodeId}`)
