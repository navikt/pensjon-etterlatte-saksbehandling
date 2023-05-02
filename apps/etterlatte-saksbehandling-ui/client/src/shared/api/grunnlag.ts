import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Grunnlagsopplysning, PersonMedNavn, Soeskenjusteringsgrunnlag } from '~shared/types/grunnlag'
import { KildePdl } from '~shared/types/kilde'

export const lagreSoeskenMedIBeregning = async (args: {
  behandlingsId: string
  soeskenMedIBeregning: { foedselsnummer: string; skalBrukes: boolean }[]
}): Promise<ApiResponse<any>> => {
  return apiClient.post(`/grunnlag/beregningsgrunnlag/${args.behandlingsId}`, {
    soeskenMedIBeregning: args.soeskenMedIBeregning,
  })
}

export const hentSoeskenjusteringsgrunnlag = async (
  sakId: number
): Promise<ApiResponse<Grunnlagsopplysning<Soeskenjusteringsgrunnlag, KildePdl> | null>> => {
  return apiClient.get(`/grunnlag/${sakId}/SOESKEN_I_BEREGNINGEN`)
}

export const hentPersonerISak = async (sakId: number): Promise<ApiResponse<PersonerISakResponse>> => {
  return apiClient.get(`/grunnlag/${sakId}/personer/alle`)
}

export type PersonerISakResponse = {
  personer: Record<string, PersonMedNavn>
}
