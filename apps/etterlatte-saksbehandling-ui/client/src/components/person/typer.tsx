import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IBoddEllerArbeidetUtlandet,
  IUtenlandstilsnitt,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { SakType } from '~shared/types/sak'

export interface IPersonResult {
  fornavn: string
  mellomnavn?: string
  etternavn: string
  foedselsnummer: string
}

export interface IBehandlingListe {
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
  vilkaarsvurderingUtfall?: VilkaarsvurderingResultat
  utenlandstilsnitt?: IUtenlandstilsnitt
  boddEllerArbeidetUtlandet?: IBoddEllerArbeidetUtlandet
}

export enum AarsaksTyper {
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
  SOEKNAD = 'SOEKNAD',
  REVURDERING = 'REVURDERING',
}

export type BehandlingOgRevurderingsAarsakerType = Revurderingsaarsak | AarsaksTyper

export type GrunnlagendringshendelseSamsvarType = SamsvarMellomKildeOgGrunnlag['type']

export enum GrunnlagsendringsType {
  DOEDSFALL = 'DOEDSFALL',
  UTFLYTTING = 'UTFLYTTING',
  FORELDER_BARN_RELASJON = 'FORELDER_BARN_RELASJON',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT = 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT',
  GRUNNBELOEP = 'GRUNNBELOEP',
  INSTITUSJONSOPPHOLD = 'INSTITUSJONSOPPHOLD',
}

export const GRUNNLAGSENDRING_STATUS = [
  'VENTER_PAA_JOBB',
  'SJEKKET_AV_JOBB',
  'TATT_MED_I_BEHANDLING',
  'VURDERT_SOM_IKKE_RELEVANT',
  'FORKASTET',
  'HISTORISK',
] as const

export const STATUS_IRRELEVANT: GrunnlagsendringStatus = 'VURDERT_SOM_IKKE_RELEVANT'
export const TATT_MED_I_BEHANDLING: GrunnlagsendringStatus = 'TATT_MED_I_BEHANDLING'

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
  fraPdl?: VergemaalEllerFremtidsfullmakt[]
  fraGrunnlag?: VergemaalEllerFremtidsfullmakt[]
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
  startdato: Date
  faktiskSluttdato: Date
  forventetSluttdato?: Date
  institusjonsType?: string
  institusjonsnavn?: string
}

export type SamsvarMellomKildeOgGrunnlag =
  | DoedsdatoSamsvar
  | UtlandSamsvar
  | BarnSamsvar
  | AnsvarligeForeldreSamsvar
  | VergemaalEllerFremtidsfullmaktForholdSamsvar
  | ReguleringSamsvar
  | InstitusjonsoppholdSamsvar

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
  samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag
  kommentar?: string
}

export interface VedtakSammendrag {
  id: string
  behandlingId: string
  datoAttestert?: string
}
