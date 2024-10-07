import { SanksjonType } from '~shared/types/sanksjon'

export interface IAvkorting {
  avkortingGrunnlag: IAvkortingGrunnlagFrontend[]
  avkortetYtelse: IAvkortetYtelse[]
  tidligereAvkortetYtelse: IAvkortetYtelse[]
}

export interface IAvkortingGrunnlagFrontend {
  aar: number
  fraVirk: IAvkortingGrunnlag | null
  historikk: IAvkortingGrunnlag[]
}

export interface IAvkortingGrunnlag {
  id: string
  fom: string
  tom?: string
  aarsinntekt: number
  fratrekkInnAar: number
  relevanteMaanederInnAar: number
  inntektUtland: number
  fratrekkInnAarUtland: number
  spesifikasjon: string
  kilde: {
    tidspunkt: string
    ident: string
  }
}

export interface IAvkortingGrunnlagLagre {
  id?: string
  aarsinntekt?: number
  fratrekkInnAar?: number
  inntektUtland?: number
  fratrekkInnAarUtland?: number
  spesifikasjon: string
  fom?: string
  overstyrtInnvilgaMaaneder?: IOverstyrtInnvilgaMaaneder
}

export interface IOverstyrtInnvilgaMaaneder {
  antall: number
  aarsak: IOverstyrtInnvilaMaanederAarsak
  begrunnelse: string
}

export enum IOverstyrtInnvilaMaanederAarsak {
  TAR_UT_PENSJON_TIDLIG = 'TAR_UT_PENSJON_TIDLIG',
  BLIR_67 = 'BLIR_67',
  ANNEN = 'ANNEN',
}

export function innvilgaMaanederType(type: IOverstyrtInnvilaMaanederAarsak) {
  switch (type) {
    case IOverstyrtInnvilaMaanederAarsak.TAR_UT_PENSJON_TIDLIG:
      return 'Tar ut pensjon tidlig'
    case IOverstyrtInnvilaMaanederAarsak.BLIR_67:
      return 'Blir 67'
    case IOverstyrtInnvilaMaanederAarsak.ANNEN:
      return 'Annen'
  }
}

export interface IAvkortetYtelse {
  id: string
  fom: string
  tom: string
  type: string
  ytelseFoerAvkorting: number
  avkortingsbeloep: number
  restanse: number
  ytelseEtterAvkorting: number
  sanksjon?: SanksjonertYtelse
}

export interface SanksjonertYtelse {
  id: string
  sanksjonType: SanksjonType
}
