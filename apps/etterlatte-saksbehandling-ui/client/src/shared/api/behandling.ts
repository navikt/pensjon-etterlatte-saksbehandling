import {
  IBoddEllerArbeidetUtlandet,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IUtlandstilknytning,
  NyBehandlingRequest,
  ViderefoertOpphoer,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { apiClient, ApiResponse } from './apiClient'
import { Grunnlagsendringshendelse, GrunnlagsendringsListe } from '~components/person/typer'
import { FoersteVirk, ISak } from '~shared/types/sak'
import { format } from 'date-fns'
import { DatoFormat } from '~utils/formatering/dato'
import { BrevutfallOgEtterbetaling } from '~components/behandling/brevutfall/Brevutfall'
import { RedigertFamilieforhold } from '~shared/types/grunnlag'
import { ISendBrev } from '~components/behandling/brevutfall/SkalSendeBrev'
import { InstitusjonsoppholdBegrunnelse } from '~components/person/hendelser/institusjonsopphold/VurderInstitusjonsoppholdModalBody'
import { JaNei } from '~shared/types/ISvar'

export const hentGrunnlagsendringshendelserForSak = async (
  sakId: number
): Promise<ApiResponse<GrunnlagsendringsListe>> => {
  return apiClient.get(`/sak/${sakId}/grunnlagsendringshendelser`)
}

export const arkiverGrunnlagshendelse = async (hendelse: Grunnlagsendringshendelse): Promise<ApiResponse<void>> => {
  return apiClient.post(`/personer/arkivergrunnlagsendringshendelse`, { ...hendelse })
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

export const lagreUtlandstilknytning = async ({
  behandlingId,
  begrunnelse,
  svar,
}: {
  behandlingId: string
  begrunnelse: string
  svar: string
}): Promise<ApiResponse<IUtlandstilknytning>> => {
  return apiClient.post(`/behandling/${behandlingId}/utlandstilknytning`, {
    utlandstilknytningType: svar,
    begrunnelse: begrunnelse,
  })
}

export const lagreViderefoertOpphoer = async ({
  skalViderefoere,
  behandlingId,
  begrunnelse,
  vilkaar,
  kravdato,
  opphoerstidspunkt,
}: {
  skalViderefoere: JaNei | undefined
  behandlingId: string
  begrunnelse: string
  vilkaar: string | undefined
  kravdato: string | null | undefined
  opphoerstidspunkt: Date | null
}): Promise<ApiResponse<ViderefoertOpphoer>> => {
  return apiClient.post(`/behandling/${behandlingId}/viderefoert-opphoer`, {
    skalViderefoere: skalViderefoere,
    vilkaar: vilkaar,
    begrunnelse: begrunnelse,
    kravdato: kravdato,
    dato: opphoerstidspunkt,
  })
}

export const slettViderefoertOpphoer = async (args: { behandlingId: string }): Promise<ApiResponse<void>> =>
  apiClient.delete(`/behandling/${args.behandlingId}/viderefoert-opphoer`)

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

export const oppdaterGrunnlag = async (args: { behandlingId: string }): Promise<ApiResponse<void>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/oppdater-grunnlag`, {})
}

export const redigerFamilieforhold = async (args: {
  behandlingId: string
  redigert: RedigertFamilieforhold
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/rediger-familieforhold`, {
    ...args.redigert,
  })
}

export const redigerSendeBrev = async (args: {
  behandlingId: string
  sendBrev: ISendBrev
}): Promise<ApiResponse<void>> => {
  const { behandlingId, sendBrev } = args
  return apiClient.put(`/behandling/${behandlingId}/skal-sende-brev`, { ...sendBrev })
}

export const lagreBrevutfallApi = async (args: {
  behandlingId: string
  brevutfall: BrevutfallOgEtterbetaling
}): Promise<ApiResponse<BrevutfallOgEtterbetaling>> => {
  return apiClient.post(`/behandling/${args.behandlingId}/info/brevutfall`, { ...args.brevutfall })
}

export const hentBrevutfallOgEtterbetalingApi = async (
  behandlingId: string
): Promise<ApiResponse<BrevutfallOgEtterbetaling | null>> => {
  return apiClient.get(`/behandling/${behandlingId}/info/brevutfallogetterbetaling`)
}
