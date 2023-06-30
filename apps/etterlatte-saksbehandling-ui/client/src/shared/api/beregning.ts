import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Beregning, BeregningsGrunnlagDto, BeregningsGrunnlagPostDto } from '~shared/types/Beregning'

export const hentBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.get(`/beregning/${behandlingId}`)
}

export const opprettEllerEndreBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.post(`/beregning/${behandlingId}`, {})
}

export const opprettBeregningForOpphoer = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.post(`/beregning/opprettForOpphoer/${behandlingId}`, {})
}

export const lagreBeregningsGrunnlag = async (args: {
  behandlingsId: string
  grunnlag: BeregningsGrunnlagPostDto
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/beregning/beregningsgrunnlag/${args.behandlingsId}/barnepensjon`, { ...args.grunnlag })
}

export const hentBeregningsGrunnlag = async (
  behandlingsId: string
): Promise<ApiResponse<BeregningsGrunnlagDto | null>> => {
  return apiClient.get<BeregningsGrunnlagDto | null>(`/beregning/beregningsgrunnlag/${behandlingsId}/barnepensjon`)
}
