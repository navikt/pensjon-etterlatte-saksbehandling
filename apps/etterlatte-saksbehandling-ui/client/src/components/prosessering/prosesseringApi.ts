import { apiClient, ApiResponse } from '~shared/api/apiClient'

export type ProsesseringStatus = 'KLAR' | 'KJØRER' | 'FULLFØRT' | 'STOPPET' | 'AVBRUTT'

export const PROSESSERING_STATUSER: ProsesseringStatus[] = ['KLAR', 'KJØRER', 'FULLFØRT', 'STOPPET', 'AVBRUTT']

export interface ProsesseringTask {
  id: number
  type: string
  status: ProsesseringStatus
  antallFeil: number
  stoppaarsak: string | null
  triggerTid: string
  opprettetTid: string
  plukketTid: string | null
  payload: string | null
}

export const kanRekjores = (status: ProsesseringStatus): boolean => status === 'STOPPET' || status === 'AVBRUTT'

export const hentProsesseringTasks = (args: {
  status?: ProsesseringStatus
  limit?: number
}): Promise<ApiResponse<ProsesseringTask[]>> => {
  const params = new URLSearchParams()
  if (args.status) params.set('status', args.status)
  if (args.limit) params.set('limit', String(args.limit))

  const query = params.toString()
  return apiClient.get<ProsesseringTask[]>(`/prosessering/task${query ? `?${query}` : ''}`)
}

export const rekjorProsesseringTask = (id: number): Promise<ApiResponse<void>> =>
  apiClient.put<void>(`/prosessering/task/${id}/rekjor`, {})

export interface FeilbarDemoResponse {
  taskId: number
  simulertOppeFra: string
}

export const opprettFeilbarDemoTask = (args: { vinduSekunder?: number }): Promise<ApiResponse<FeilbarDemoResponse>> =>
  apiClient.post<FeilbarDemoResponse>('/prosessering/demo/feilbar', {
    vinduSekunder: args.vinduSekunder ?? 20,
  })
