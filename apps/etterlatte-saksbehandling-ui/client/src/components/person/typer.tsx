import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'

export interface Dokument {
  dato: string
  tittel: string
  link: string
  status: string
}

export interface Dokumenter {
  brev: Dokument[]
}

export interface IPersonInfo {
  navn: string
  fnr: string
  type: string
}

export interface IPersonResult {
  fornavn: string
  etternavn: string
  foedselsnummer: string
}

export interface IBehandlingListe {
  behandlinger: IBehandlingsammendrag[]
}

export interface IBehandlingsammendrag {
  id: string
  sak: number
  status: IBehandlingStatus
  soeknadMottattDato: string
  behandlingOpprettet: string
  behandlingType: IBehandlingsType
  aarsak: AarsaksTyper
}

export enum AarsaksTyper {
  SOEKER_DOD = 'SOEKER_DOD',
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
  SOEKNAD = 'SOEKNAD',
}

export type GrunnlagsendringsType = SamsvarMellomGrunnlagOgPdl['type']

const GRUNNLAGSENDRING_STATUS = [
  'VENTER_PAA_JOBB',
  'SJEKKET_AV_JOBB',
  'TATT_MED_I_BEHANDLING',
  'VURDERT_SOM_IKKE_RELEVANT',
  'FORKASTET',
] as const

interface Utland {
  utflyttingFraNorge?: {
    tilflyttingsland?: string
    dato?: string
  }[]
  innflyttingTilNorge?: {
    fraflyttingsland?: string
    dato?: string
  }[]
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

export interface AnsvarligeForeldreSamsvar {
  type: 'ANSVARLIGE_FORELDRE'
  samsvar: boolean
  fraPdl?: string[]
  fraGrunnlag?: string[]
}

export type SamsvarMellomGrunnlagOgPdl = DoedsdatoSamsvar | UtlandSamsvar | BarnSamsvar | AnsvarligeForeldreSamsvar

export type GrunnlagsendringStatus = typeof GRUNNLAGSENDRING_STATUS[number]

const SAKSROLLER = ['SOEKER', 'INNSENDER', 'SOESKEN', 'AVDOED', 'GJENLEVENDE', 'UKJENT'] as const

export type Saksrolle = typeof SAKSROLLER[number]

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
  samsvarMellomPdlOgGrunnlag: SamsvarMellomGrunnlagOgPdl
}
