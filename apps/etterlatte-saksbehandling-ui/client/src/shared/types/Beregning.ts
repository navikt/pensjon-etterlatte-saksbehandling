import {
  PeriodisertBeregningsgrunnlag,
  PeriodisertBeregningsgrunnlagDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { KildeSaksbehandler } from '~shared/types/kilde'

export interface Beregning {
  beregningId: string
  behandlingId: string
  type: Beregningstype
  beregningsperioder: Beregningsperiode[]
  beregnetDato: string
  grunnlagMetadata: GrunnlagMetadata
}

export interface GrunnlagMetadata {
  sakId: string
  versjon: number
}

export enum Beregningstype {
  BP = 'BP',
  OMS = 'OMS',
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
}

export interface BeregningsGrunnlagDto {
  behandlingId: string
  kilde: KildeSaksbehandler
  institusjonsoppholdBeregningsgrunnlag: InstitusjonsoppholdGrunnlagDTO
  soeskenMedIBeregning: SoeskenMedIBeregningGrunnlagDto
}

export interface BeregningsGrunnlagOMSDto {
  behandlingId: string
  kilde: KildeSaksbehandler
  institusjonsoppholdBeregningsgrunnlag: InstitusjonsoppholdGrunnlagDTO
}

export type InstitusjonsoppholdGrunnlagDTO = PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>[]

export interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}

export interface BeregningsGrunnlagPostDto {
  soeskenMedIBeregning: SoeskenMedIBeregningGrunnlagDto
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
}

export interface BeregningsGrunnlagOMSPostDto {
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
}

export type SoeskenMedIBeregningGrunnlagDto = PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[]
export type InstitusjonsoppholdGrunnlagData = PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning>[]

export interface InstitusjonsoppholdIBeregning {
  reduksjon: Reduksjonstypekey
  egenReduksjon?: string | null
  begrunnelse?: string | null
}

export const Reduksjon = {
  VELG_REDUKSJON: 'Velg reduksjon',
  JA_VANLIG: 'Ja, etter vanlig sats(10% av G)',
  NEI_KORT_OPPHOLD: 'Nei, kort opphold',
  JA_EGEN_PROSENT_AV_G: 'Ja, utgifter til bolig(egen % av G)',
  NEI_HOEYE_UTGIFTER_BOLIG: 'Nei, har høye utgifter til bolig',
}

type Reduksjonstypekey = keyof typeof Reduksjon
