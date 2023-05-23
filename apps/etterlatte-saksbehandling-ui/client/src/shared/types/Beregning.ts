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
  grunnbelopMnd: number
  grunnbelop: number
  trygdetid: number
}

export interface BeregningsGrunnlag<K> {
  behandlingId: string
  kilde: K
  institusjonsopphold: Institusjonsopphold
  soeskenMedIBeregning: PeriodisertBeregningsgrunnlag<SoeskenMedIBeregning[]>[]
}

export interface BeregningsGrunnlagDto {
  behandlingId: string
  kilde: KildeSaksbehandler
  institusjonsopphold: Institusjonsopphold
  soeskenMedIBeregning: PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[]
}

export interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}

export interface BeregningsGrunnlagData {
  soeskenMedIBeregning: PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[]
  institusjonsopphold: Institusjonsopphold
}

export interface Institusjonsopphold {
  institusjonsopphold: boolean
}
