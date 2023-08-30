import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  Beregning,
  BeregningsGrunnlagDto,
  BeregningsGrunnlagOMSDto,
  BeregningsGrunnlagOMSPostDto,
  BeregningsGrunnlagPostDto,
} from '~shared/types/Beregning'

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

export const lagreBeregningsGrunnlagOMS = async (args: {
  behandlingsId: string
  grunnlag: BeregningsGrunnlagOMSPostDto
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/beregning/beregningsgrunnlag/${args.behandlingsId}/omstillingstoenad`, { ...args.grunnlag })
}

export const hentBeregningsGrunnlag = async (
  behandlingsId: string
): Promise<ApiResponse<BeregningsGrunnlagDto | null>> => {
  return apiClient.get<BeregningsGrunnlagDto | null>(`/beregning/beregningsgrunnlag/${behandlingsId}/barnepensjon`)
}

export const hentBeregningsGrunnlagOMS = async (
  behandlingsId: string
): Promise<ApiResponse<BeregningsGrunnlagOMSDto | null>> => {
  return apiClient.get<BeregningsGrunnlagOMSDto | null>(
    `/beregning/beregningsgrunnlag/${behandlingsId}/omstillingstoenad`
  )
}
