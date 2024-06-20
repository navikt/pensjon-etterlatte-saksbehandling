import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  Beregning,
  BeregningsGrunnlagDto,
  BeregningsGrunnlagOMSDto,
  BeregningsGrunnlagOMSPostDto,
  BeregningsGrunnlagPostDto,
  OverstyrBeregning,
  OverstyrBeregningGrunnlagPostDTO,
} from '~shared/types/Beregning'
import { KATEGORI } from '~shared/types/OverstyrtBeregning'

export const hentBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.get(`/beregning/${behandlingId}`)
}

export const opprettEllerEndreBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.post(`/beregning/${behandlingId}`, {})
}

export const lagreBeregningsGrunnlag = async (args: {
  behandlingId: string
  grunnlag: BeregningsGrunnlagPostDto
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/beregning/beregningsgrunnlag/${args.behandlingId}/barnepensjon`, { ...args.grunnlag })
}

export const lagreBeregningsGrunnlagOMS = async (args: {
  behandlingId: string
  grunnlag: BeregningsGrunnlagOMSPostDto
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/beregning/beregningsgrunnlag/${args.behandlingId}/omstillingstoenad`, { ...args.grunnlag })
}

export const hentBeregningsGrunnlag = async (
  behandlingId: string
): Promise<ApiResponse<BeregningsGrunnlagDto | null>> => {
  return apiClient.get<BeregningsGrunnlagDto | null>(`/beregning/beregningsgrunnlag/${behandlingId}/barnepensjon`)
}

export const hentBeregningsGrunnlagOMS = async (
  behandlingId: string
): Promise<ApiResponse<BeregningsGrunnlagOMSDto | null>> => {
  return apiClient.get<BeregningsGrunnlagOMSDto | null>(
    `/beregning/beregningsgrunnlag/${behandlingId}/omstillingstoenad`
  )
}

export const hentOverstyrBeregning = async (behandlingId: string): Promise<ApiResponse<OverstyrBeregning | null>> => {
  return apiClient.get<OverstyrBeregning | null>(`/beregning/${behandlingId}/overstyrt`)
}

export const slettOverstyrtBeregning = async (behandlingId: string): Promise<ApiResponse<void>> => {
  return apiClient.delete<void>(`/beregning/${behandlingId}/overstyrt`)
}

export const opprettOverstyrBeregning = async (args: {
  behandlingId: string
  beskrivelse: string
  kategori: KATEGORI
}): Promise<ApiResponse<OverstyrBeregning | null>> => {
  return apiClient.post<OverstyrBeregning | null>(`/beregning/${args.behandlingId}/overstyrt`, {
    beskrivelse: args.beskrivelse,
    kategori: args.kategori,
  })
}

export const hentOverstyrBeregningGrunnlag = async (
  behandlingId: string
): Promise<ApiResponse<OverstyrBeregningGrunnlagPostDTO>> => {
  return apiClient.get(`/beregning/beregningsgrunnlag/${behandlingId}/overstyr`)
}

export const lagreOverstyrBeregningGrunnlag = async (args: {
  behandlingId: string
  grunnlag: OverstyrBeregningGrunnlagPostDTO
}): Promise<ApiResponse<OverstyrBeregningGrunnlagPostDTO>> => {
  return apiClient.post(`/beregning/beregningsgrunnlag/${args.behandlingId}/overstyr`, { ...args.grunnlag })
}
