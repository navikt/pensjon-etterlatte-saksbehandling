import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { OppdatertOppgaveversjonResponseDto, RedigerFristRequest, SaksbehandlerEndringDto } from '~shared/api/oppgaver'
import { OppgaveDTO } from '~shared/types/oppgave'
import { GosysFilter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { GosysOppgave } from '~shared/types/Gosys'

export const hentGosysOppgaver = async (filter: GosysFilter): Promise<ApiResponse<GosysOppgave[]>> => {
  const queryParams = new URLSearchParams({
    saksbehandler: filter.saksbehandlerFilter || '',
    tema: filter.temaFilter || '',
    enhet: filter.enhetFilter ? filter.enhetFilter.replace('E', '') : '',
  })

  return apiClient.get(`/oppgaver/gosys?${queryParams}`)
}

export const tildelSaksbehandlerApi = async (args: {
  oppgaveId: number
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
}

export const redigerFristApi = async (args: {
  oppgaveId: number
  redigerFristRequest: RedigerFristRequest
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/endre-frist`, { ...args.redigerFristRequest })
}

export const ferdigstilleGosysOppgave = async (args: {
  oppgaveId: number
  versjon: number
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/ferdigstill?versjon=${args.versjon}`, {})

export const feilregistrerGosysOppgave = async (args: {
  oppgaveId: number
  beskrivelse: string
  versjon: number
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/feilregistrer`, {
    versjon: args.versjon,
    beskrivelse: args.beskrivelse,
  })
