import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'

export const lagreBosattutland = async ({
  bosattutland,
  behandlingId,
}: {
  bosattutland: Bosattutland
  behandlingId: string
}): Promise<ApiResponse<Bosattutland>> => {
  return apiClient.post(`/bosattutland/${behandlingId}`, { ...bosattutland })
}

export const hentBosattutland = async (behandlingId: string): Promise<ApiResponse<Bosattutland>> => {
  return apiClient.get(`/bosattutland/${behandlingId}`)
}

export interface Bosattutland {
  behandlingId: string
  rinanummer: string
  mottatteSeder: LandMedDokumenter[]
  sendteSeder: LandMedDokumenter[]
}
