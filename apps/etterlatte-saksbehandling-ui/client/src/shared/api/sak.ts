import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IUtenlandstilknytning } from '~shared/types/IDetaljertBehandling'
import { ISak, ISakMedUtenlandstilknytning } from '~shared/types/sak'

export const lagreUtenlandstilknytning = async ({
  sakId,
  begrunnelse,
  svar,
}: {
  sakId: number
  begrunnelse: string
  svar: string
}): Promise<ApiResponse<IUtenlandstilknytning>> => {
  return apiClient.post(`/sak/${sakId}/utenlandstilknytning`, {
    utenlandstilknytningType: svar,
    begrunnelse: begrunnelse,
  })
}

export const hentSakMedUtenlandstilknytning = async (
  fnr: string
): Promise<ApiResponse<ISakMedUtenlandstilknytning>> => {
  return apiClient.post(`/personer/utenlandstilknytning`, { foedselsnummer: fnr })
}

export const hentSak = async (sakId: number): Promise<ApiResponse<ISak>> => {
  return apiClient.get(`sak/${sakId}`)
}
