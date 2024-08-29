import {
  PeriodisertBeregningsgrunnlag,
  PeriodisertBeregningsgrunnlagDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { IProrataBroek } from '~shared/api/trygdetid'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'

export interface Beregning {
  beregningId: string
  behandlingId: string
  type: Beregningstype
  beregningsperioder: Beregningsperiode[]
  beregnetDato: string
  grunnlagMetadata: GrunnlagMetadata
  overstyrBeregning: OverstyrBeregning | undefined
}

export interface OverstyrBeregning {
  beskrivelse: string
  kategori: OverstyrtBeregningKategori
}

export interface GrunnlagMetadata {
  sakId: string
  versjon: number
}

export enum Beregningstype {
  BP = 'BP',
  OMS = 'OMS',
}

export enum BeregningsMetode {
  BEST = 'BEST',
  NASJONAL = 'NASJONAL',
  PRORATA = 'PRORATA',
}

export interface BeregningsMetodeBeregningsgrunnlag {
  beregningsMetode: BeregningsMetode | null
  begrunnelse?: string | null
}

export interface BeregningsmetodeForAvdoed {
  beregningsMetode: BeregningsMetodeBeregningsgrunnlag
  avdoed: string
}

export interface Beregningsperiode {
  delytelsesId: string
  datoFOM: string
  datoTOM: string
  utbetaltBeloep: number
  soeskenFlokk: string[]
  institusjonsopphold: InstitusjonsoppholdIBeregning | undefined
  grunnbelopMnd: number
  grunnbelop: number
  trygdetid: number
  beregningsMetode: string | undefined
  samletNorskTrygdetid: number | undefined
  samletTeoretiskTrygdetid: number | undefined
  broek: IProrataBroek | undefined
  avdoedeForeldre: string[] | undefined
}

// TODO: Burde speile backend DTO-en
export interface OverstyrBeregningsperiode {
  utbetaltBeloep: string
  trygdetid: string
  trygdetidForIdent: string | undefined
  prorataBroekTeller: string | undefined
  prorataBroekNevner: string | undefined
  beskrivelse: string
  aarsak: OverstyrtAarsakKey | undefined
}

export interface BeregningsGrunnlagDto {
  behandlingId: string
  kilde: KildeSaksbehandler
  institusjonsoppholdBeregningsgrunnlag: InstitusjonsoppholdGrunnlagDTO
  soeskenMedIBeregning: SoeskenMedIBeregningGrunnlagDto
  beregningsMetode: BeregningsMetodeBeregningsgrunnlag
  beregningsMetodeFlereAvdoede: BeregningsmetodeFlereAvdoedeDTO
}

export type InstitusjonsoppholdGrunnlagDTO = PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>[]

export interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}

export type OverstyrBeregningGrunnlagDTO = PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>[]

export interface OverstyrBeregningGrunnlagPostDTO {
  perioder: OverstyrBeregningGrunnlagDTO
}

export interface BeregningsGrunnlagPostDto {
  soeskenMedIBeregning: SoeskenMedIBeregningGrunnlagDto
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
  beregningsMetode: BeregningsMetodeBeregningsgrunnlag
  beregningsMetodeFlereAvdoede: BeregningsmetodeFlereAvdoedeDTO | undefined
}

export interface BeregningsGrunnlagOMSPostDto {
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
  beregningsMetode: BeregningsMetodeBeregningsgrunnlag
}

export type SoeskenMedIBeregningGrunnlagDto = PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[]
export type InstitusjonsoppholdGrunnlagData = PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning>[]
export type BeregningsmetodeFlereAvdoedeDTO = PeriodisertBeregningsgrunnlagDto<BeregningsmetodeForAvdoed>[]

export interface InstitusjonsoppholdIBeregning {
  reduksjon: ReduksjonKey
  egenReduksjon?: string | null
  begrunnelse?: string | null
}

type ReduksjonKeyBP =
  | 'VELG_REDUKSJON'
  | 'JA_VANLIG'
  | 'NEI_KORT_OPPHOLD'
  | 'JA_EGEN_PROSENT_AV_G'
  | 'NEI_HOEYE_UTGIFTER_BOLIG'

type ReduksjonKeyOMS =
  | 'VELG_REDUKSJON'
  | 'JA_VANLIG_OMS'
  | 'NEI_KORT_OPPHOLD'
  | 'JA_EGEN_PROSENT_AV_G'
  | 'NEI_HOEYE_UTGIFTER_BOLIG'
  | 'NEI_OMSORG_BARN'

export type ReduksjonKey = ReduksjonKeyBP | ReduksjonKeyOMS

export type ReduksjonType = Record<string, string>

export const ReduksjonBP: ReduksjonType = {
  VELG_REDUKSJON: 'Velg reduksjon',
  JA_VANLIG: 'Ja, etter vanlig sats(10% av G)',
  JA_FORELDRELOES: 'Ja, etter vanlig sats - foreldreløs (45% av G)',
  NEI_KORT_OPPHOLD: 'Nei, kort opphold',
  JA_EGEN_PROSENT_AV_G: 'Ja, utgifter til bolig(egen % av G)',
  NEI_HOEYE_UTGIFTER_BOLIG: 'Nei, har høye utgifter til bolig',
}

export const ReduksjonOMS: ReduksjonType = {
  VELG_REDUKSJON: 'Velg reduksjon',
  JA_VANLIG_OMS: 'Ja, etter vanlig sats(45% av G)',
  NEI_KORT_OPPHOLD: 'Nei, kort opphold',
  JA_EGEN_PROSENT_AV_G: 'Ja, utgifter til bolig(egen % av G)',
  NEI_HOEYE_UTGIFTER_BOLIG: 'Nei, har høye utgifter til bolig',
  NEI_OMSORG_BARN: 'Nei, har omsorg for barn',
}

export type OverstyrtAarsakType = Record<string, string>

export type OverstyrtAarsakKey = 'VELG_AARSAK' | 'AVKORTET_UFOERETRYGD' | 'AVKORTET_FENGSEL' | 'ANNET'

export const OverstyrtAarsak: OverstyrtAarsakType = {
  VELG_AARSAK: 'Velg årsak',
  AVKORTET_UFOERETRYGD: 'Avkortet pga uføretrygd',
  AVKORTET_FENGSEL: 'Avkortet pga fengsel',
  ANNET: 'Annet',
}
