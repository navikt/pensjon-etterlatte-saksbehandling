import { IBehandlingStatus, IBehandlingsType } from '~store/reducers/BehandlingReducer'

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
  person: {
    fornavn: string
    etternavn: string
    foedselsnummer: string
  }
  behandlingListe: {
    behandlinger: IBehandlingsammendrag[]
  }
  grunnlagsendringshendelser?: {
    hendelser: Grunnlagsendringshendelse[]
  }
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

export interface Utflyttingshendelse {
  fnr: string
  tilflyttingsLand?: string
  tilflyttingsstedIUtlandet?: string
  utflyttingsdato: string
  endringstype: Endringstype
}

export interface Doedshendelse {
  avdoedFnr: string
  doedsdato?: string
  endringstype: Endringstype
}

export type Grunnlagsinformasjon =
  | { type: 'SOEKER_DOED'; hendelse: Doedshendelse }
  | { type: 'SOESKEN_DOED'; hendelse: Doedshendelse }
  | { type: 'GJENLEVENDE_FORELDER_DOED'; hendelse: Doedshendelse }
  | { type: 'UTFLYTTING'; hendelse: Utflyttingshendelse }

export type GrunnlagsendringsType = Grunnlagsinformasjon['type']

export const ENDRINGSTYPER = ['OPPRETTET', 'KORRIGERT', 'ANNULERT', 'OPPHOERT'] as const
export type Endringstype = typeof ENDRINGSTYPER[number]

const GRUNNLAGSENDRING_STATUS = [
  'IKKE_VURDERT',
  'TATT_MED_I_BEHANDLING',
  'GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING',
  'FORKASTET',
] as const

export type GrunnlagsendringStatus = typeof GRUNNLAGSENDRING_STATUS[number]

export interface Grunnlagsendringshendelse {
  id: string
  sakId: number
  type: GrunnlagsendringsType
  data?: Grunnlagsinformasjon
  status: GrunnlagsendringStatus
  behandlingId?: string
  opprettet: string
}
