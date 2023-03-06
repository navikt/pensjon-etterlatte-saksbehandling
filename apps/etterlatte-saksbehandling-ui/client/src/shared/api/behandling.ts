import {
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { apiClient, ApiResponse } from './apiClient'
import { ManueltOpphoerDetaljer } from '~components/behandling/manueltopphoeroversikt/ManueltOpphoerOversikt'

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
  begrunnelse: string
}): Promise<ApiResponse<Virkningstidspunkt>> => {
  return apiClient.post(`/behandling/${args.id}/virkningstidspunkt`, {
    dato: args.dato,
    begrunnelse: args.begrunnelse,
  })
}

export const hentManueltOpphoerDetaljer = async (
  behandlingId: string
): Promise<ApiResponse<ManueltOpphoerDetaljer>> => {
  return apiClient.get(`/behandling/${behandlingId}/manueltopphoer`)
}

export const fattVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingsId}/fattvedtak`, {})
}

export const upsertVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingsId}/upsert`, {})
}

export const attesterVedtak = async (behandlingId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingId}/attester`, {})
}

export const underkjennVedtak = async (
  behandlingId: string,
  kommentar: string,
  valgtBegrunnelse: string
): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingId}/underkjenn`, { kommentar, valgtBegrunnelse })
}
export const lagreGyldighetsproeving = async (args: {
  behandlingId: string
  svar: string
  begrunnelse: string
}): Promise<ApiResponse<IGyldighetResultat>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/gyldigfremsatt`, {
    svar: args.svar,
    begrunnelse: args.begrunnelse,
  })
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

interface GrunnlagResponse {
  response: string
}

export const slettPeriodeForAvdoedesMedlemskap = async (
  behandlingsId: string,
  saksbehandlerPeriodeId: string
): Promise<ApiResponse<GrunnlagResponse>> =>
  apiClient.delete<GrunnlagResponse>(`/grunnlag/saksbehandler/periode/${behandlingsId}/${saksbehandlerPeriodeId}`)
