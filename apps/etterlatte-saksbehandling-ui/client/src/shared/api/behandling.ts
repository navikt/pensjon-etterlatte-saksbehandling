import { IDetaljertBehandling, Virkningstidspunkt } from '../../store/reducers/BehandlingReducer'
import { apiClient, ApiResponse } from './apiClient'

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
  return apiClient.post(`/fattvedtak/${behandlingsId}`, {})
}

export const attesterVedtak = async (behandlingId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/attestervedtak/${behandlingId}`, {})
}

export const underkjennVedtak = async (
  behandlingId: string,
  kommentar: string,
  valgtBegrunnelse: string
): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`underkjennVedtak/${behandlingId}`, { kommentar, valgtBegrunnelse })
}

export const lagreBegrunnelseKommerBarnetTilgode = async (
  behandlingsId: string,
  begrunnelse: string,
  svar: string
): Promise<ApiResponse<any>> => {
  return apiClient.post(`/behandling/${behandlingsId}/kommerbarnettilgode`, { svar, begrunnelse }, true)
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
