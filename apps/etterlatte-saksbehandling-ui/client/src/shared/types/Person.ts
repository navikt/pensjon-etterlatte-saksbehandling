import { Personopplysning } from '~shared/types/grunnlag'
import { IAdresse } from '~shared/types/IAdresse'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'
import { SakType } from '~shared/types/sak'

export interface Familieforhold {
  soeker: Personopplysning | undefined
  avdoede: Personopplysning[]
  gjenlevende: Personopplysning[]
}

export const hentLevendeSoeskenFraAvdoedeForSoeker = (avdoede: Personopplysning[], soekerFnr?: string) => {
  const alleAvdoedesBarn = avdoede.flatMap((avdoed) => avdoed.opplysning.avdoedesBarn ?? [])
  const levendeSoesken = alleAvdoedesBarn.filter((barn) => barn.foedselsnummer !== soekerFnr && !barn.doedsdato)
  const unikeSoesken = levendeSoesken.filter(
    (b, index, arr) => index === arr.findIndex((t) => t?.foedselsnummer === b.foedselsnummer)
  )
  return unikeSoesken
}

export interface IFamilieRelasjon {
  ansvarligeForeldre?: string[]
  foreldre?: string[]
  barn?: string[]
}

export interface IPdlPersonSoekResponse {
  fornavn: string
  mellomnavn?: string
  etternavn: string
  foedselsnummer: string
  bostedsadresse?: IAdresse[]
}

export interface IPdlPerson {
  fornavn: string
  mellomnavn?: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: Date
  foedselsaar: number
  doedsdato: Date | undefined
  bostedsadresse?: IAdresse[]
  deltBostedsadresse?: IAdresse[]
  kontaktadresse?: IAdresse[]
  oppholdsadresse?: IAdresse[]
  avdoedesBarn?: IPdlPerson[]
  utland?: Utland
  statsborgerskap?: string
  pdlStatsborgerskap?: Statsborgerskap[]
  familieRelasjon?: IFamilieRelasjon
  sivilstatus?: Sivilstatus
  sivilstand?: Sivilstand[]
  vergemaalEllerFremtidsfullmakt?: VergemaalEllerFremtidsfullmakt[]
}

export interface IPdlPersonNavnFoedsel {
  fornavn: string
  mellomnavn?: string
  etternavn: string
  foedselsnummer: string
  historiskeFoedselsnummer: string[]
  foedselsaar: number
  foedselsdato: Date | undefined
  doedsdato: Date | undefined
  vergemaal: VergemaalEllerFremtidsfullmakt | undefined
}

export interface Utland {
  utflyttingFraNorge?: {
    tilflyttingsland?: string
    dato?: string
  }[]
  innflyttingTilNorge?: InnflyttingDTO[]
}

export interface InnflyttingDTO {
  fraflyttingsland?: string
  dato?: string
  gyldighetsdato?: string
  ajourholdsdato?: string
}

export interface Statsborgerskap {
  land: string
  gyldigFraOgMed?: string
  gyldigTilOgMed?: string
}

export interface Sivilstand {
  sivilstatus: Sivilstatus
  relatertVedSiviltilstand?: string
  gyldigFraOgMed?: Date
  bekreftetDato?: Date
  historisk: boolean
  kilde: string
}

enum Sivilstatus {
  UOPPGITT = 'UOPPGITT',
  UGIFT = 'UGIFT',
  GIFT = 'GIFT',
  ENKE_ELLER_ENKEMANN = 'ENKE_ELLER_ENKEMANN',
  SKILT = 'SKILT',
  SEPARERT = 'SEPARERT',
  REGISTRERT_PARTNER = 'REGISTRERT_PARTNER',
  SEPARERT_PARTNER = 'SEPARERT_PARTNER',
  SKILT_PARTNER = 'SKILT_PARTNER',
  GJENLEVENDE_PARTNER = 'GJENLEVENDE_PARTNER',
}

export const formaterSivilstatusTilLesbarStreng = (sivilstatus: Sivilstatus | undefined) => {
  switch (sivilstatus) {
    case Sivilstatus.UOPPGITT:
      return 'Uoppgitt'
    case Sivilstatus.UGIFT:
      return 'Ugift'
    case Sivilstatus.GIFT:
      return 'Gift'
    case Sivilstatus.ENKE_ELLER_ENKEMANN:
      return 'Enke eller enkemann'
    case Sivilstatus.SKILT:
      return 'Skilt'
    case Sivilstatus.SEPARERT:
      return 'Separert'
    case Sivilstatus.REGISTRERT_PARTNER:
      return 'Registrert partner'
    case Sivilstatus.SEPARERT_PARTNER:
      return 'Separert partner'
    case Sivilstatus.SKILT_PARTNER:
      return 'Skilt partner'
    case Sivilstatus.GJENLEVENDE_PARTNER:
      return 'Gjenlevende partner'
    default:
      return 'Uoppgitt'
  }
}

export function formaterNavn(person: PersonNavn): string {
  return [person.fornavn, person.mellomnavn, person.etternavn].filter((navn) => !!navn).join(' ')
}

export const formaterAdresse = (adresse: IAdresse): string => {
  return `${adresse.adresseLinje1 ?? '-'}, ${adresse.postnr ?? '-'} ${adresse.poststed ?? '-'}`
}

export interface Persongalleri {
  soeker?: string
  innsender?: string | null
  soesken?: string[]
  avdoed?: string[]
  gjenlevende?: string[]
  personerUtenIdent?: PersonUtenIdent[] | null
}

export type RelativPersonrolle = 'FORELDER' | 'BARN'

export interface PersonUtenIdent {
  rolle: RelativPersonrolle
  person: RelatertPerson
}

export const relativPersonrolleTekst: Record<SakType, Record<RelativPersonrolle, string>> = {
  [SakType.BARNEPENSJON]: {
    BARN: 'Søsken',
    FORELDER: 'Forelder',
  },
  [SakType.OMSTILLINGSSTOENAD]: {
    BARN: 'Barn',
    FORELDER: 'Voksen / forelder',
  },
}

export type PersonNavn = Pick<IPdlPerson, 'fornavn' | 'mellomnavn' | 'etternavn'>

export interface RelatertPerson {
  foedselsdato?: string
  kjoenn?: string
  navn?: Partial<PersonNavn>
  statsborgerskap?: string
}
