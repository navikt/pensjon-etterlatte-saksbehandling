import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  AktivitetspliktOppfoelgingsOppgave,
  AktivitetspliktOppfolging,
  AktivitetspliktOppgaveVurdering,
  AktivitetspliktOppgaveVurderingType,
  IAktivitetHendelse,
  IAktivitetPeriode,
  IAktivitetPerioderOgHendelser,
  IAktivitetspliktVurderingNyDto,
  IOpprettAktivitetspliktAktivitetsgrad,
  IOpprettAktivitetspliktUnntak,
  OpprettAktivitetPeriode,
  OpprettAktivitetHendelse,
} from '~shared/types/Aktivitetsplikt'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { OppgaveDTO } from '~shared/types/oppgave'
import { Spraak } from '~shared/types/Brev'

/*
    Denne ligger her for å hente ut data som ble vurdert på gammel flyt
 */
export const hentAktivitetspliktOppfolging = async (args: {
  behandlingId: string
}): Promise<ApiResponse<AktivitetspliktOppfolging>> => apiClient.get(`/behandling/${args.behandlingId}/aktivitetsplikt`)

export const opprettAktivitetPeriodeForBehandling = async (args: {
  behandlingId: string
  request: OpprettAktivitetPeriode
}): Promise<ApiResponse<IAktivitetPeriode[]>> =>
  apiClient.post(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet`, { ...args.request })

export const opprettAktivitetPeriodeForSak = async (args: {
  sakId: number
  request: OpprettAktivitetPeriode
}): Promise<ApiResponse<IAktivitetPeriode[]>> =>
  apiClient.post(`/sak/${args.sakId}/aktivitetsplikt/aktivitet`, { ...args.request })

export const opprettAktivitetHendelseForBehandling = async (args: {
  behandlingId: string
  request: OpprettAktivitetHendelse
}): Promise<ApiResponse<IAktivitetHendelse[]>> =>
  apiClient.post(`/behandling/${args.behandlingId}/aktivitetsplikt/hendelse`, { ...args.request })

export const opprettAktivitetHendelseForSak = async (args: {
  sakId: number
  request: OpprettAktivitetHendelse
}): Promise<ApiResponse<IAktivitetHendelse[]>> =>
  apiClient.post(`/sak/${args.sakId}/aktivitetsplikt/hendelse`, { ...args.request })

export const slettAktivitetPeriodeForBehandling = async (args: {
  behandlingId: string
  aktivitetPeriodeId: string
}): Promise<ApiResponse<IAktivitetPeriode[]>> =>
  apiClient.delete(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet/${args.aktivitetPeriodeId}`)

export const hentAktiviteterOgHendelser = async (args: {
  sakId: number
  behandlingId?: string
}): Promise<ApiResponse<IAktivitetPerioderOgHendelser>> => {
  const parameter = args.behandlingId ? `?behandlingId=${args.behandlingId}` : ''
  return apiClient.get(`/sak/${args.sakId}/aktivitetsplikt/aktivitet-og-hendelser${parameter}`)
}

export const slettAktivitetPeriodeForSak = async (args: {
  sakId: number
  aktivitetPeriodeId: string
}): Promise<ApiResponse<IAktivitetPeriode[]>> =>
  apiClient.delete(`/sak/${args.sakId}/aktivitetsplikt/aktivitet/${args.aktivitetPeriodeId}`)

export const slettAktivitetHendelseForSak = async (args: {
  sakId: number
  aktivitetHendelseId: string
}): Promise<ApiResponse<IAktivitetHendelse[]>> =>
  apiClient.delete(`/sak/${args.sakId}/aktivitetsplikt/hendelse/${args.aktivitetHendelseId}`)

export const hentAktivitspliktVurderingForSak = async (args: {
  sakId: number
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.get(`/sak/${args.sakId}/aktivitetsplikt/vurdering`)

export const hentOppfoelgingsoppgaver = async (args: {
  sakId: number
}): Promise<ApiResponse<AktivitetspliktOppfoelgingsOppgave[]>> =>
  apiClient.get(`/sak/${args.sakId}/aktivitetsplikt/oppgaver-sak`)

export const opprettOppfoelgingsoppgave = async (args: {
  sakId: number
  vurderingType: AktivitetspliktOppgaveVurderingType
}) =>
  apiClient.post(`/sak/${args.sakId}/aktivitetsplikt/opprett`, {
    sakId: args.sakId,
    type: args.vurderingType,
  })

export const redigerAktivitetsgradForOppgave = async (args: {
  sakId: number
  oppgaveId: string
  request: IOpprettAktivitetspliktAktivitetsgrad
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.post(`/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering/aktivitetsgrad`, {
    ...args.request,
  })

export interface AktvitetspliktAktivitetsgradOgUnntak {
  aktivitetsgrad: IOpprettAktivitetspliktAktivitetsgrad
  unntak?: IOpprettAktivitetspliktUnntak
}

export const opprettAktivitetspliktAktivitetsgradOgUnntakForOppgave = async (args: {
  sakId: number
  oppgaveId: string
  request: AktvitetspliktAktivitetsgradOgUnntak
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.post(`/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering/aktivitetsgrad-og-unntak`, {
    ...args.request,
  })

export const slettAktivitetsgradForOppgave = async (args: {
  sakId: number
  oppgaveId: string
  aktivitetsgradId: string
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.delete(
    `/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering/aktivitetsgrad/${args.aktivitetsgradId}`
  )

export const redigerAktivitetspliktUnntakForOppgave = async (args: {
  sakId: number
  oppgaveId: string
  request: IOpprettAktivitetspliktUnntak
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.post(`/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering/unntak`, { ...args.request })

export const slettAktivitetspliktUnntakForOppgave = async (args: {
  sakId: number
  oppgaveId: string
  unntakId: string
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.delete(`/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering/unntak/${args.unntakId}`)

export const hentAktivitspliktVurderingForBehandling = async (args: {
  sakId: number
  behandlingId: string
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.get(`/sak/${args.sakId}/behandling/${args.behandlingId}/aktivitetsplikt/vurdering`)

export const opprettAktivitspliktAktivitetsgradOgUnntakForBehandling = async (args: {
  sakId: number
  behandlingId: string
  request: AktvitetspliktAktivitetsgradOgUnntak
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.post(`/sak/${args.sakId}/behandling/${args.behandlingId}/aktivitetsplikt/vurdering/aktivitetsgrad-unntak`, {
    ...args.request,
  })

export const redigerAktivitetsgradForBehandling = async (args: {
  sakId: number
  behandlingId: string
  request: IOpprettAktivitetspliktAktivitetsgrad
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.post(`/sak/${args.sakId}/behandling/${args.behandlingId}/aktivitetsplikt/vurdering/aktivitetsgrad`, {
    ...args.request,
  })

export const slettAktivitetsgradForBehandling = async (args: {
  sakId: number
  behandlingId: string
  aktivitetsgradId: string
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.delete(
    `/sak/${args.sakId}/behandling/${args.behandlingId}/aktivitetsplikt/vurdering/aktivitetsgrad/${args.aktivitetsgradId}`
  )

export const redigerUnntakForBehandling = async (args: {
  sakId: number
  behandlingId: string
  request: IOpprettAktivitetspliktUnntak
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.post(`/sak/${args.sakId}/behandling/${args.behandlingId}/aktivitetsplikt/vurdering/unntak`, {
    ...args.request,
  })

export const slettUnntakForBehandling = async (args: {
  sakId: number
  behandlingId: string
  unntakId: string
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.delete(
    `/sak/${args.sakId}/behandling/${args.behandlingId}/aktivitetsplikt/vurdering/unntak/${args.unntakId}`
  )

export const hentAktivitetspliktOppgaveVurdering = async (args: {
  oppgaveId: string
}): Promise<ApiResponse<AktivitetspliktOppgaveVurdering>> => apiClient.get(`/aktivitetsplikt/oppgave/${args.oppgaveId}`)

export interface IBrevAktivitetspliktRequest {
  skalSendeBrev: boolean
  utbetaling?: boolean
  redusertEtterInntekt?: boolean
  spraak?: Spraak
  begrunnelse?: string
}

export const lagreAktivitetspliktBrevdata = async (args: {
  oppgaveId: string
  brevdata: IBrevAktivitetspliktRequest
}): Promise<ApiResponse<IBrevAktivitetspliktDto>> =>
  apiClient.post(`/aktivitetsplikt/oppgave/${args.oppgaveId}/brevdata`, { ...args.brevdata })

export const opprettAktivitetspliktsbrev = async (args: { oppgaveId: string }): Promise<ApiResponse<BrevId>> =>
  apiClient.post(`/aktivitetsplikt/oppgave/${args.oppgaveId}/opprettbrev`, {})

export const ferdigstillBrevOgOppgaveAktivitetsplikt = async (args: {
  oppgaveId: string
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/aktivitetsplikt/oppgave/${args.oppgaveId}/ferdigstillbrev-og-oppgave`, {})

export const ferdigstillOppgaveUtenBrevAktivitetsplikt = async (args: {
  oppgaveId: string
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/aktivitetsplikt/oppgave/${args.oppgaveId}/ferdigstill-oppgave`, {})

interface BrevId {
  brevId: number
}

export interface IBrevAktivitetspliktDto {
  brevId?: number
  skalSendeBrev: boolean
  utbetaling?: boolean
  redusertEtterInntekt?: boolean
  spraak?: Spraak
  begrunnelse?: string
  kilde: KildeSaksbehandler
}
