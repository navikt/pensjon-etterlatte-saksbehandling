import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingStatus, IBehandlingsType, Virkningstidspunkt } from '~shared/types/IDetaljertBehandling'

export interface Dokument {
  dato: string
  tittel: string
  link: string
  status: string
}

export interface Dokumenter {
  brev: Dokument[]
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
  virkningstidspunkt?: Virkningstidspunkt
  vilkaarsvurderingUtfall?: VilkaarsvurderingResultat
}

export enum AarsaksTyper {
  SOEKER_DOD = 'SOEKER_DOD',
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
  SOEKNAD = 'SOEKNAD',
  REGULERING = 'REGULERING',
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

export interface VergemaalEllerFremtidsfullmaktForholdSamsvar {
  type: 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT'
  samsvar: boolean
  fraPdl?: string[]
  fraGrunnlag?: string[]
}

export interface ReguleringSamsvar {
  type: 'REGULERING'
  samsvar: boolean
}

export type SamsvarMellomGrunnlagOgPdl =
  | DoedsdatoSamsvar
  | UtlandSamsvar
  | BarnSamsvar
  | AnsvarligeForeldreSamsvar
  | VergemaalEllerFremtidsfullmaktForholdSamsvar
  | ReguleringSamsvar

export type GrunnlagsendringStatus = (typeof GRUNNLAGSENDRING_STATUS)[number]

const SAKSROLLER = ['SOEKER', 'INNSENDER', 'SOESKEN', 'AVDOED', 'GJENLEVENDE', 'UKJENT'] as const

export type Saksrolle = (typeof SAKSROLLER)[number]

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

export interface VedtakSammendrag {
  id: string
  behandlingId: string
  datoAttestert?: string
}
