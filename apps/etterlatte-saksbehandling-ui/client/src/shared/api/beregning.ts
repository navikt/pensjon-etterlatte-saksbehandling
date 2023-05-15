import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Beregning, BeregningsGrunnlag, Institusjonsopphold, SoeskenMedIBeregning } from '~shared/types/Beregning'
import { KildeSaksbehandler } from '~shared/types/kilde'

import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'

export const hentBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.get(`/beregning/${behandlingId}`)
}

export const opprettEllerEndreBeregning = async (behandlingId: string): Promise<ApiResponse<Beregning>> => {
  return apiClient.post(`/beregning/${behandlingId}`, {})
}

export const lagreBeregningsGrunnlag = async (args: {
  behandlingsId: string
  soeskenMedIBeregning: PeriodisertBeregningsgrunnlag<SoeskenMedIBeregning[]>[]
  institusjonsopphold: Institusjonsopphold
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/beregning/beregningsgrunnlag/${args.behandlingsId}/barnepensjon`, {
    soeskenMedIBeregning: args.soeskenMedIBeregning,
    institusjonsopphold: args.institusjonsopphold,
  })
}

export const hentBeregningsGrunnlag = async (
  behandlingsId: string
): Promise<ApiResponse<BeregningsGrunnlag<KildeSaksbehandler> | null>> => {
  return apiClient.get(`/beregning/beregningsgrunnlag/${behandlingsId}/barnepensjon`)
}
