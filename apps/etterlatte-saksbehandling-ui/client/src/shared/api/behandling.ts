import {
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IUtenlandstilsnitt,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { apiClient, ApiResponse } from './apiClient'
import { ManueltOpphoerDetaljer } from '~components/behandling/manueltopphoeroversikt/ManueltOpphoerOversikt'
import { Grunnlagsendringshendelse } from '~components/person/typer'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { ISaksType } from '~components/behandling/fargetags/saksType'

export const hentBehandlingerForPerson = async (fnr: string): Promise<ApiResponse<any>> => {
  return apiClient.post(`/personer/behandlinger`, { foedselsnummer: fnr })
}

export const hentGrunnlagsendringshendelserForPerson = async (fnr: string): Promise<ApiResponse<any>> => {
  return apiClient.post(`/personer/grunnlagsendringshendelser`, { foedselsnummer: fnr })
}

export const lukkGrunnlagshendelse = async (hendelse: Grunnlagsendringshendelse): Promise<ApiResponse<any>> => {
  return apiClient.post(`/personer/lukkgrunnlagsendringshendelse`, { ...hendelse })
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

export const attesterVedtak = async (args: {
  behandlingId: string
  kommentar: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${args.behandlingId}/attester`, { kommentar: args.kommentar })
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

export const opprettRevurdering = async (args: {
  sakId: number
  aarsak: Revurderingsaarsak
}): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.post(`/${args.sakId}/revurdering`, {
    aarsak: args.aarsak,
  })
}

export const hentStoettedeRevurderinger = async (args: {
  sakType: ISaksType
}): Promise<ApiResponse<Array<Revurderingsaarsak>>> => {
  return apiClient.get(`/stoettederevurderinger/${args.sakType}`)
}

export const lagreUtenlandstilsnitt = async (args: {
  behandlingId: string
  begrunnelse: string
  svar: string
}): Promise<ApiResponse<IUtenlandstilsnitt>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/utenlandstilsnitt`, {
    utenlandstilsnittType: args.svar,
    begrunnelse: args.begrunnelse,
  })
}

export const hentGrunnlagsendringshendelserInstitusjonsoppholdforSak = async (
  sakid: number
): Promise<ApiResponse<Array<Grunnlagsendringshendelse>>> => {
  return apiClient.get(`/grunnlagsendringshendelse/${sakid}/institusjonshendelser`)
}
