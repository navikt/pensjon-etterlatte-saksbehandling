import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IUtenlandstilknytning } from '~shared/types/IDetaljertBehandling'
import { ISak, SakType } from '~shared/types/sak'
import { SakMedBehandlinger } from '~components/person/typer'

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

export const hentSakMedBehandlnger = async (fnr: string): Promise<ApiResponse<SakMedBehandlinger>> => {
  return apiClient.post(`/personer/behandlingerforsak`, { foedselsnummer: fnr })
}

export const hentSak = async (sakId: number): Promise<ApiResponse<ISak>> => {
  return apiClient.get(`sak/${sakId}`)
}

export const hentSakForPerson = async (args: { fnr: string; type: SakType }): Promise<ApiResponse<ISak>> =>
  apiClient.post(`/personer/sak/${args.type}`, { foedselsnummer: args.fnr })
