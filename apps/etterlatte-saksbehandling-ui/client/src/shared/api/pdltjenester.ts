import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IPdlPerson, IPdlPersonNavn } from '~shared/types/Person'
import { SakType } from '~shared/types/sak'

/**
 * Hent navn til person med ident.
 * Identen kan være:
 * - Fødselsnummer
 * - AktørID
 * - NPID
 **/

export interface PersonopplysningerDTO {
  soeker?: IPdlPerson
  avdoede?: IPdlPerson[]
  gjenlevende?: IPdlPerson[]
}

export const hentPerson = async (args: {
  ident: string
  sakType: SakType
}): Promise<ApiResponse<PersonopplysningerDTO>> => apiClient.post('/pdltjenester/person/opplysninger', args)

export const hentPersonNavn = async (ident: string): Promise<ApiResponse<IPdlPersonNavn>> =>
  apiClient.post(`/pdltjenester/person/navn`, { ident })
