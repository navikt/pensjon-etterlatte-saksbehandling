import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Grunnlagsopplysning, Soeskenjusteringsgrunnlag } from '~shared/types/Grunnlagsopplysning'

export const lagreSoeskenMedIBeregning = async (args: {
  behandlingsId: string
  soeskenMedIBeregning: { foedselsnummer: string; skalBrukes: boolean }[]
}): Promise<ApiResponse<any>> => {
  return apiClient.post(`/grunnlag/beregningsgrunnlag/${args.behandlingsId}`, {
    soeskenMedIBeregning: args.soeskenMedIBeregning,
  })
}

export const hentSoeskenMedIBeregning = async (
  sakId: number
): Promise<ApiResponse<Grunnlagsopplysning<Soeskenjusteringsgrunnlag>>> => {
  return apiClient.get(`/grunnlag/${sakId}/SOESKEN_I_BEREGNINGEN`)
}
