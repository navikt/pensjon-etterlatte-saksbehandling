import {
  IBehandlingStatus,
  IBehandlingsType,
  IBoddEllerArbeidetUtlandet,
  Vedtaksloesning,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { ISakMedUtlandstilknytning, SakType } from '~shared/types/sak'
import { IAdresse } from '~shared/types/IAdresse'
import { Utland } from '~shared/types/Person'

export interface IPersonResult {
  fornavn: string
  mellomnavn?: string
  etternavn: string
  foedselsnummer: string
}

export interface SakMedBehandlinger {
  sak: ISakMedUtlandstilknytning
  behandlinger: IBehandlingsammendrag[]
}

export interface IBehandlingsammendrag {
  id: string
  sak: number
  sakType: SakType
  status: IBehandlingStatus
  soeknadMottattDato: string
  behandlingOpprettet: string
  behandlingType: IBehandlingsType
  aarsak: BehandlingOgRevurderingsAarsakerType
  virkningstidspunkt?: Virkningstidspunkt
  boddEllerArbeidetUtlandet?: IBoddEllerArbeidetUtlandet
  kilde: Vedtaksloesning
}

export enum AarsaksTyper {
  SOEKNAD = 'SOEKNAD',
  REVURDERING = 'REVURDERING',
}

export type BehandlingOgRevurderingsAarsakerType = Revurderingaarsak | AarsaksTyper

export type GrunnlagendringshendelseSamsvarType = SamsvarMellomKildeOgGrunnlag['type']

export enum GrunnlagsendringsType {
  DOEDSFALL = 'DOEDSFALL',
  UTFLYTTING = 'UTFLYTTING',
  FORELDER_BARN_RELASJON = 'FORELDER_BARN_RELASJON',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT = 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT',
  SIVILSTAND = 'SIVILSTAND',
  GRUNNBELOEP = 'GRUNNBELOEP',
  INSTITUSJONSOPPHOLD = 'INSTITUSJONSOPPHOLD',
  ADRESSE = 'ADRESSE',
}

export interface Folkeregisteridentifikatorsamsvar {
  type: 'FOLKEREGISTERIDENTIFIKATOR'
  samsvar: boolean
}

export interface DoedsdatoSamsvar {
  type: 'DOEDSDATO'
  samsvar: boolean
  fraPdl?: string
  fraGrunnlag?: string
}

export interface UtlandSamsvar {
  type: 'UTLAND'
  samsvar: boolean
  fraPdl?: Utland
  fraGrunnlag?: Utland
}

export interface BarnSamsvar {
  type: 'BARN'
  samsvar: boolean
  fraPdl?: string[]
  fraGrunnlag?: string[]
}

export interface AdresseSamsvar {
  type: 'ADRESSE'
  samsvar: boolean
  fraPdl: IAdresse[]
  fraGrunnlag: IAdresse[]
}

export interface AnsvarligeForeldreSamsvar {
  type: 'ANSVARLIGE_FORELDRE'
  samsvar: boolean
  fraPdl?: string[]
  fraGrunnlag?: string[]
}

export interface VergemaalEllerFremtidsfullmaktForholdSamsvar {
  type: 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT'
  samsvar: boolean
  fraPdl?: VergemaalEllerFremtidsfullmakt[]
  fraGrunnlag?: VergemaalEllerFremtidsfullmakt[]
}

export interface SivilstandSamsvar {
  type: 'SIVILSTAND'
  samsvar: boolean
  fraPdl?: Sivilstand[]
  fraGrunnlag?: Sivilstand[]
}

export interface Sivilstand {
  sivilstatus: string
  relatertVedSiviltilstand?: string
  gyldigFraOgMed?: string
  bekreftelsesdato?: string
  kilde: string
}

export interface VergemaalEllerFremtidsfullmakt {
  embete?: string
  type?: string
  vergeEllerFullmektig: VergeEllerFullmektig
}

export interface VergeEllerFullmektig {
  motpartsPersonident?: string
  navn?: string
  omfang?: string
  omfangetErInnenPersonligOmraade: boolean
}

export interface ReguleringSamsvar {
  type: 'GRUNNBELOEP'
  samsvar: boolean
}

export interface InstitusjonsoppholdSamsvar {
  type: 'INSTITUSJONSOPPHOLD'
  samsvar: boolean
  oppholdstype: string
  oppholdBeriket: InstitusjonsoppholdHendelseBeriket
}

interface InstitusjonsoppholdHendelseBeriket {
  startdato: string
  faktiskSluttdato: string
  organisasjonsnummer?: string
  forventetSluttdato?: string
  institusjonsType?: string
  institusjonsnavn?: string
}

export type SamsvarMellomKildeOgGrunnlag =
  | DoedsdatoSamsvar
  | UtlandSamsvar
  | BarnSamsvar
  | AnsvarligeForeldreSamsvar
  | VergemaalEllerFremtidsfullmaktForholdSamsvar
  | SivilstandSamsvar
  | ReguleringSamsvar
  | InstitusjonsoppholdSamsvar
  | AdresseSamsvar
  | Folkeregisteridentifikatorsamsvar

export enum GrunnlagsendringStatus {
  VENTER_PAA_JOBB = 'VENTER_PAA_JOBB',
  SJEKKET_AV_JOBB = 'SJEKKET_AV_JOBB',
  TATT_MED_I_BEHANDLING = 'TATT_MED_I_BEHANDLING',
  VURDERT_SOM_IKKE_RELEVANT = 'VURDERT_SOM_IKKE_RELEVANT',
  FORKASTET = 'FORKASTET',
  HISTORISK = 'HISTORISK',
}

export enum Saksrolle {
  SOEKER = 'SOEKER',
  INNSENDER = 'INNSENDER',
  SOESKEN = 'SOESKEN',
  AVDOED = 'AVDOED',
  GJENLEVENDE = 'GJENLEVENDE',
  UKJENT = 'UKJENT',
}

export interface GrunnlagsendringsListe {
  hendelser: Grunnlagsendringshendelse[]
}

export interface Grunnlagsendringshendelse {
  id: string
  sakId: number
  type: GrunnlagsendringsType
  status: GrunnlagsendringStatus
  behandlingId?: string
  opprettet: string
  hendelseGjelderRolle: Saksrolle
  gjelderPerson: string
  samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag
  kommentar?: string
}
