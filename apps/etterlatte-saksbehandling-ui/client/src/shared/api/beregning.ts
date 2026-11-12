import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  Beregning,
  BeregningsGrunnlagDto,
  Grunnbeloep,
  LagreBeregningsGrunnlagDto,
  OverstyrBeregning,
  OverstyrBeregningGrunnlagPostDTO,
} from '~shared/types/Beregning'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'
import { beregningConfig } from '~clientConfig'

export const beregningApiUrl = (sti = ''): string => {
  return `${beregningConfig.brukBehandling ? '/beregning_v2' : '/beregning'}${sti}`
}

export const hentBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.get(beregningApiUrl(`/${behandlingId}`))
}

export const opprettEllerEndreBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.post(beregningApiUrl(`/${behandlingId}`), {})
}

export const lagreBeregningsGrunnlag = async (args: {
  behandlingId: string
  grunnlag: LagreBeregningsGrunnlagDto | undefined
}): Promise<ApiResponse<BeregningsGrunnlagDto>> => {
  return apiClient.post(beregningApiUrl(`/beregningsgrunnlag/${args.behandlingId}`), { ...args.grunnlag })
}

export const hentBeregningsGrunnlag = async (
  behandlingId: string
): Promise<ApiResponse<BeregningsGrunnlagDto | null>> => {
  return apiClient.get<BeregningsGrunnlagDto | null>(beregningApiUrl(`/beregningsgrunnlag/${behandlingId}`))
}

export const hentOverstyrBeregning = async (behandlingId: string): Promise<ApiResponse<OverstyrBeregning | null>> => {
  return apiClient.get<OverstyrBeregning | null>(beregningApiUrl(`/${behandlingId}/overstyrt`))
}

export const deaktiverOverstyrtBeregning = async (behandlingId: string): Promise<ApiResponse<void>> => {
  return apiClient.delete<void>(beregningApiUrl(`/${behandlingId}/overstyrt`))
}

export const opprettOverstyrBeregning = async (args: {
  behandlingId: string
  beskrivelse: string
  kategori: OverstyrtBeregningKategori
}): Promise<ApiResponse<OverstyrBeregning | null>> => {
  return apiClient.post<OverstyrBeregning | null>(beregningApiUrl(`/${args.behandlingId}/overstyrt`), {
    beskrivelse: args.beskrivelse,
    kategori: args.kategori,
  })
}

export const hentOverstyrBeregningGrunnlag = async (
  behandlingId: string
): Promise<ApiResponse<OverstyrBeregningGrunnlagPostDTO>> => {
  return apiClient.get(beregningApiUrl(`/beregningsgrunnlag/${behandlingId}/overstyr`))
}

export const lagreOverstyrBeregningGrunnlag = async (args: {
  behandlingId: string
  grunnlag: OverstyrBeregningGrunnlagPostDTO
}): Promise<ApiResponse<OverstyrBeregningGrunnlagPostDTO>> => {
  return apiClient.post(beregningApiUrl(`/beregningsgrunnlag/${args.behandlingId}/overstyr`), { ...args.grunnlag })
}

export async function hentGjeldendeGrunnbeloep(): Promise<ApiResponse<Grunnbeloep>> {
  return apiClient.get(beregningApiUrl('/grunnbeloep'))
}
