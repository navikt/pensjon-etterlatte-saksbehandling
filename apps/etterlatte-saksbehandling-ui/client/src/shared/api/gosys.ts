import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { OppgaveDTO } from '~shared/types/oppgave'
import { GosysFilter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { GosysOppgave } from '~shared/types/Gosys'

export const hentGosysOppgaver = async (filter: GosysFilter): Promise<ApiResponse<GosysOppgave[]>> => {
  const queryParams = new URLSearchParams({
    saksbehandler: filter.saksbehandlerFilter || '',
    tema: filter.temaFilter || '',
    enhet: !!filter.enhetFilter && filter.enhetFilter !== 'visAlle' ? filter.enhetFilter.replace('E', '') : '',
    harTildeling: filter.harTildelingFilter === false ? filter.harTildelingFilter.toString() : '',
  })

  return apiClient.get(`/oppgaver/gosys?${queryParams}`)
}

export const hentGosysOppgaverForPerson = async (ident: string): Promise<ApiResponse<GosysOppgave[]>> => {
  return apiClient.post(`/oppgaver/gosys/person`, { foedselsnummer: ident })
}

export const hentJournalfoeringsoppgaverFraGosys = async (
  journalpostId: string
): Promise<ApiResponse<GosysOppgave[]>> => apiClient.get(`/oppgaver/gosys/journalfoering/${journalpostId}`)

export const tildelSaksbehandlerApi = async (args: {
  oppgaveId: number
  nysaksbehandler: SaksbehandlerEndringDto
  enhetsnr: string
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, {
    ...args.nysaksbehandler,
    enhetsnr: args.enhetsnr,
  })
}

export const flyttTilGjenny = async (args: {
  oppgaveId: number
  sakId: number
  enhetsnr: string
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/flytt-til-gjenny`, { sakid: args.sakId, enhetsnr: args.enhetsnr })

export const ferdigstilleGosysOppgave = async (args: {
  oppgaveId: number
  versjon: number
  enhetsnr: string
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/ferdigstill`, { versjon: args.versjon, enhetsnr: args.enhetsnr })

export interface OppdatertOppgaveversjonResponseDto {
  versjon: number
}

export interface SaksbehandlerEndringDto {
  saksbehandler: string
  versjon: number
}
