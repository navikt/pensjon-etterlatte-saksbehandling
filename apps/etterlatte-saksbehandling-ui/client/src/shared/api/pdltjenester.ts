import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IPdlPersonNavn, Utland } from '~shared/types/Person'
import { SakType } from '~shared/types/sak'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'

export interface PersonopplysningerForenklet {
  soeker?: PersonopplysningPerson
  avdoede?: PersonopplysningPerson[]
  gjenlevende?: PersonopplysningPerson[]
}

export interface PersonopplysningPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato?: Date
  doedsdato?: Date
  bostedsadresse?: Bostedsadresse[]
  sivilstand?: Sivilstand[]
  statsborgerskap?: string
  pdlStatsborgerskap?: PdlStatsborgerskap[]
  utland?: Utland
  familierelasjon?: Familierelasjon
  avdoedesBarn?: PersonopplysningPerson[]
  vergemaalEllerFremtidsfullmakt?: VergemaalEllerFremtidsfullmakt[]
}

export interface Bostedsadresse {
  adresse?: string
  postnr?: string
  gyldigFraOgMed?: string
  gyldigTilOgMed?: string
  aktiv: boolean
}

export interface Sivilstand {
  sivilstatus: string
  relatertVedSivilstand?: string
  gyldigFraOgMed?: Date
}

export interface PdlStatsborgerskap {
  land: string
  gyldigFraOgMed?: string
  gyldigTilOgMed?: string
}

export interface Familierelasjon {
  ansvarligeForeldre?: string[]
  barn?: string[]
}

/**
 * Hent navn til person med ident.
 * Identen kan være:
 * - Fødselsnummer
 * - AktørID
 * - NPID
 **/
export const hentPersonNavn = async (ident: string): Promise<ApiResponse<IPdlPersonNavn>> =>
  apiClient.post(`/pdltjenester/person/navn`, { ident })

export const hentPersonopplysninger = async (args: {
  ident: string
  sakType: SakType
}): Promise<ApiResponse<PersonopplysningerForenklet>> => apiClient.post('/pdltjenester/person/opplysninger', args)
