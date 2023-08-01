import {
  IBoddEllerArbeidetUtlandet,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IUtenlandstilsnitt,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { apiClient, ApiResponse } from './apiClient'
import { ManueltOpphoerDetaljer } from '~components/behandling/manueltopphoeroversikt/ManueltOpphoerOversikt'
import { Grunnlagsendringshendelse, GrunnlagsendringsListe, IBehandlingListe } from '~components/person/typer'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { InstitusjonsoppholdBegrunnelse } from '~components/person/uhaandtereHendelser/InstitusjonsoppholdVurderingBegrunnelse'
import { FoersteVirk, ISak, SakType } from '~shared/types/sak'
import { InstitusjonsoppholdMedKilde } from '~components/person/uhaandtereHendelser/HistoriskeHendelser'

export const hentBehandlingerForPerson = async (fnr: string): Promise<ApiResponse<IBehandlingListe[]>> => {
  return apiClient.post(`/personer/behandlinger`, { foedselsnummer: fnr })
}

export const hentGrunnlagsendringshendelserForPerson = async (
  fnr: string
): Promise<ApiResponse<GrunnlagsendringsListe[]>> => {
  return apiClient.post(`/personer/grunnlagsendringshendelser`, { foedselsnummer: fnr })
}

export const lukkGrunnlagshendelse = async (hendelse: Grunnlagsendringshendelse): Promise<ApiResponse<any>> => {
  return apiClient.post(`/personer/lukkgrunnlagsendringshendelse`, { ...hendelse })
}

export const hentBehandling = async (id: string): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.get(`/behandling/${id}`)
}

export const annullerBehandling = async (id: string): Promise<ApiResponse<unknown>> => {
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

export const lagreInstitusjonsoppholdData = async (args: {
  sakId: number
  institusjonsopphold: InstitusjonsoppholdBegrunnelse
}): Promise<ApiResponse<InstitusjonsoppholdBegrunnelse>> => {
  return apiClient.post(`/institusjonsoppholdbegrunnelse/${args.sakId}`, {
    institusjonsopphold: args.institusjonsopphold,
  })
}

export const hentInstitusjonsoppholdData = async (
  grunnlagsendringshendelseid: string
): Promise<ApiResponse<InstitusjonsoppholdMedKilde>> => {
  return apiClient.get(`/institusjonsoppholdbegrunnelse/${grunnlagsendringshendelseid}`)
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
  paaGrunnAvHendelseId?: string
  begrunnelse?: string
  fritekstAarsak?: string
}): Promise<ApiResponse<string>> => {
  return apiClient.post(`/revurdering/${args.sakId}`, {
    sakId: args.sakId,
    aarsak: args.aarsak,
    paaGrunnAvHendelseId: args.paaGrunnAvHendelseId,
    begrunnelse: args.begrunnelse,
    fritekstAarsak: args.fritekstAarsak,
  })
}

export const hentStoettedeRevurderinger = async (args: {
  sakType: SakType
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

export const lagreBoddEllerArbeidetUtlandet = async (args: {
  behandlingId: string
  begrunnelse: string
  svar: boolean
}): Promise<ApiResponse<IBoddEllerArbeidetUtlandet>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/boddellerarbeidetutlandet`, {
    boddEllerArbeidetUtlandet: args.svar,
    begrunnelse: args.begrunnelse,
  })
}

export const hentGrunnlagsendringshendelserInstitusjonsoppholdforSak = async (
  sakid: number
): Promise<ApiResponse<Array<Grunnlagsendringshendelse>>> => {
  return apiClient.get(`/grunnlagsendringshendelse/${sakid}/institusjon`)
}

export const hentSak = async (sakId: string): Promise<ApiResponse<ISak>> => {
  return apiClient.get(`/sak/${sakId}`)
}

export const hentFoersteVirk = async (args: { sakId: number }) =>
  apiClient.get<FoersteVirk>(`/sak/${args.sakId}/behandlinger/foerstevirk`)
