import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  AvsluttKlageRequest,
  Formkrav,
  IniteltUtfallMedBegrunnelseDto,
  Klage,
  KlageUtfallUtenBrev,
  NyKlageRequest,
} from '~shared/types/Klage'
import { OppgaveDTO } from '~shared/types/oppgave'
import { GrunnForAvslutning } from '~components/oppgavebenk/oppgaveModal/AvsluttOmgjoeringsoppgave'

export function opprettNyKlage(nyKlageRequest: NyKlageRequest): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/opprett/${nyKlageRequest.sakId}`, { ...nyKlageRequest })
}

export function lagreKlagerIkkeSvart(args: { begrunnelse: string; klageId: string }): Promise<ApiResponse<Klage>> {
  return apiClient.put(`/klage/${args.klageId}/klager-ikke-svart`, { begrunnelse: args.begrunnelse })
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
  utfallMedBegrunnelse: IniteltUtfallMedBegrunnelseDto
}): Promise<ApiResponse<Klage>> {
  const { klageId, utfallMedBegrunnelse } = args
  return apiClient.put(`/klage/${klageId}/initieltutfall`, {
    utfall: utfallMedBegrunnelse.utfall,
    begrunnelse: utfallMedBegrunnelse.begrunnelse,
  })
}

export function oppdaterMottattDatoForKlage(args: {
  klageId: string
  mottattDato: string
}): Promise<ApiResponse<Klage>> {
  const { klageId, mottattDato } = args
  return apiClient.put(`/klage/${klageId}/mottattdato`, { mottattDato })
}

export function ferdigstillKlagebehandling(klageId: string): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/${klageId}/ferdigstill`, {})
}

export function fattVedtakOmAvvistKlage(klageId: string): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/${klageId}/vedtak/fatt`, {})
}

export function attesterVedtakOmAvvistKlage(args: { klageId: string; kommentar: string }): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/${args.klageId}/vedtak/attester`, {
    kommentar: args.kommentar,
  })
}

export function opprettOppgaveForOmgjoering(args: { klageId: string }): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/${args.klageId}/vedtak/underkjenn`, {})
}

export function underkjennVedtakOmAvvistKlage(args: {
  klageId: string
  kommentar: string
  valgtBegrunnelse: string
}): Promise<ApiResponse<Klage>> {
  return apiClient.post(`/klage/${args.klageId}/vedtak/underkjenn`, {
    kommentar: args.kommentar,
    valgtBegrunnelse: args.valgtBegrunnelse,
  })
}

export function avsluttKlage(avsluttKlageRequest: AvsluttKlageRequest): Promise<ApiResponse<void>> {
  return apiClient.post(`/klage/${avsluttKlageRequest.klageId}/avbryt`, { ...avsluttKlageRequest })
}

export function forhaandsvisBlankettKa(args: { klage: Klage }): Promise<ApiResponse<ArrayBuffer>> {
  return apiClient.post<ArrayBuffer>(`/notat/sak/${args.klage.sak.id}/forhaandsvis`, {
    data: {
      type: 'KLAGE_BLANKETT',
      klage: args.klage,
    },
  })
}

export function avsluttOmgjoeringsoppgave(args: {
  oppgaveId: string
  omgjoerendeBehandling?: string
  begrunnelse: string
  hvorforAvsluttes: GrunnForAvslutning
}): Promise<ApiResponse<OppgaveDTO>> {
  return apiClient.post(`/klage/omgjoering/${args.oppgaveId}/avslutt`, {
    ...args,
  })
}
