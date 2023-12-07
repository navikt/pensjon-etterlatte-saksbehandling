import {
  IBoddEllerArbeidetUtlandet,
  IDetaljertBehandling,
  IEtterbetaling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  NyBehandlingRequest,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { apiClient, ApiResponse } from './apiClient'
import { ManueltOpphoerDetaljer } from '~components/behandling/manueltopphoeroversikt/ManueltOpphoerOversikt'
import { Grunnlagsendringshendelse, GrunnlagsendringsListe } from '~components/person/typer'
import { InstitusjonsoppholdBegrunnelse } from '~components/person/uhaandtereHendelser/InstitusjonsoppholdVurderingBegrunnelse'
import { FoersteVirk, ISak } from '~shared/types/sak'
import { InstitusjonsoppholdMedKilde } from '~components/person/uhaandtereHendelser/HistoriskeHendelser'
import { format } from 'date-fns'
import { DatoFormat } from '~utils/formattering'
import { Aldersgruppe, Brevoppsett } from '~components/behandling/brevoppsett/Brevoppsett'

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

export const opprettBehandling = async (nyBehandlingRequest: NyBehandlingRequest): Promise<ApiResponse<string>> =>
  apiClient.post(`/behandling`, { ...nyBehandlingRequest })

export const avbrytBehandling = async (id: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/behandling/${id}/avbryt`, {})
}

export const fastsettVirkningstidspunkt = async (args: {
  id: string
  dato: Date
  begrunnelse: string
  kravdato: Date | null
}): Promise<ApiResponse<Virkningstidspunkt>> => {
  return apiClient.post(`/behandling/${args.id}/virkningstidspunkt`, {
    dato: args.dato,
    begrunnelse: args.begrunnelse,
    kravdato: args.kravdato ? format(args.kravdato, DatoFormat.AAR_MAANED_DAG) : null,
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

export const lagreBoddEllerArbeidetUtlandet = async (args: {
  behandlingId: string
  begrunnelse: string
  svar: boolean
  boddArbeidetIkkeEosEllerAvtaleland?: boolean
  boddArbeidetEosNordiskKonvensjon?: boolean
  boddArbeidetAvtaleland?: boolean
  vurdereAvoededsTrygdeavtale?: boolean
  skalSendeKravpakke?: boolean
}): Promise<ApiResponse<IBoddEllerArbeidetUtlandet>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/boddellerarbeidetutlandet`, {
    boddEllerArbeidetUtlandet: args.svar,
    begrunnelse: args.begrunnelse,
    boddArbeidetIkkeEosEllerAvtaleland: args.boddArbeidetIkkeEosEllerAvtaleland,
    boddArbeidetEosNordiskKonvensjon: args.boddArbeidetEosNordiskKonvensjon,
    boddArbeidetAvtaleland: args.boddArbeidetAvtaleland,
    vurdereAvoededsTrygdeavtale: args.vurdereAvoededsTrygdeavtale,
    skalSendeKravpakke: args.skalSendeKravpakke,
  })
}

export const hentGrunnlagsendringshendelserInstitusjonsoppholdForSak = async (
  sakid: number
): Promise<ApiResponse<Array<Grunnlagsendringshendelse>>> => {
  return apiClient.get(`/grunnlagsendringshendelse/${sakid}/institusjon`)
}

export const hentSak = async (sakId: string): Promise<ApiResponse<ISak>> => {
  return apiClient.get(`/sak/${sakId}`)
}

export const hentFoersteVirk = async (args: { sakId: number }) =>
  apiClient.get<FoersteVirk>(`/sak/${args.sakId}/behandlinger/foerstevirk`)

export const lagreEtterbetaling = async (args: {
  behandlingId: string
  etterbetaling: IEtterbetaling
}): Promise<ApiResponse<void>> => {
  return apiClient.put(`/behandling/${args.behandlingId}/etterbetaling `, {
    fraDato: args.etterbetaling.fra,
    tilDato: args.etterbetaling.til,
  })
}
export const slettEtterbetaling = async (args: { behandlingId: string }): Promise<ApiResponse<void>> => {
  return apiClient.delete(`/behandling/${args.behandlingId}/etterbetaling`)
}

export const oppdaterGrunnlag = async (args: { behandlingId: string }): Promise<ApiResponse<void>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/oppdater-grunnlag`, {})
}

export const lagreBrevoppsett = async (args: {
  behandlingId: string
  brevoppsett: Brevoppsett
}): Promise<ApiResponse<Brevoppsett>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/brevoppsett`, { ...args.brevoppsett })
}

export const hentBrevoppsett = async (behandlingId: string): Promise<ApiResponse<Brevoppsett>> => {
  console.log(behandlingId)
  return new Promise<ApiResponse<Brevoppsett>>((resolve) => {
    resolve({
      ok: true,
      status: 200,
      data: { etterbetaling: { fom: new Date(), tom: new Date() }, aldersgruppe: Aldersgruppe.OVER_18 },
    })
  })
  //return apiClient.get(`/behandling/${behandlingId}/brevoppsett`)
}
