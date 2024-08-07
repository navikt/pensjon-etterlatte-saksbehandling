import { Utland } from '~shared/types/Person'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'

export interface FamilieOpplysninger {
  soeker?: Familiemedlem
  avdoede?: Familiemedlem[]
  gjenlevende?: Familiemedlem[]
}

export interface Familiemedlem {
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
  barn?: Familiemedlem[]
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
