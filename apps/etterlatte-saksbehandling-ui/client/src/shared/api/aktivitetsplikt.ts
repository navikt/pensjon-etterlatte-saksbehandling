import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  AktivitetspliktOppfolging,
  AktivitetspliktOppgaveVurdering,
  IAktivitet,
  IAktivitetspliktVurdering,
  IAktivitetspliktVurderingNyDto,
  IOpprettAktivitet,
  IOpprettAktivitetspliktAktivitetsgrad,
  IOpprettAktivitetspliktUnntak,
} from '~shared/types/Aktivitetsplikt'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { OppgaveDTO } from '~shared/types/oppgave'
import { Spraak } from '~shared/types/Brev'

export const hentAktivitetspliktOppfolging = async (args: {
  behandlingId: string
}): Promise<ApiResponse<AktivitetspliktOppfolging>> => apiClient.get(`/behandling/${args.behandlingId}/aktivitetsplikt`)

export const hentAktiviteterForBehandling = async (args: {
  behandlingId: string
}): Promise<ApiResponse<IAktivitet[]>> => apiClient.get(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet`)

export const opprettAktivitet = async (args: {
  behandlingId: string
  request: IOpprettAktivitet
}): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.post(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet`, { ...args.request })

export const slettAktivitet = async (args: {
  behandlingId: string
  aktivitetId: string
}): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.delete(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet/${args.aktivitetId}`)

export const hentAktiviteterForSak = async (args: { sakId: number }): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.get(`/sak/${args.sakId}/aktivitetsplikt/aktivitet`)

export const opprettAktivitetForSak = async (args: {
  sakId: number
  request: IOpprettAktivitet
}): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.post(`/sak/${args.sakId}/aktivitetsplikt/aktivitet`, { ...args.request })

export const slettAktivitetForSak = async (args: {
  sakId: number
  aktivitetId: string
}): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.delete(`/sak/${args.sakId}/aktivitetsplikt/aktivitet/${args.aktivitetId}`)

export const hentAktivitspliktVurderingForSak = async (args: {
  sakId: number
}): Promise<ApiResponse<IAktivitetspliktVurderingNyDto>> =>
  apiClient.get(`/sak/${args.sakId}/aktivitetsplikt/vurdering`)

export const hentAktivitspliktVurderingForOppgave = async (args: {
  sakId: number
  oppgaveId: string
}): Promise<ApiResponse<IAktivitetspliktVurdering>> =>
  apiClient.get(`/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering`)

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
