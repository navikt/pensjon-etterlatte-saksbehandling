import { SanksjonType } from '~shared/types/sanksjon'

export interface IAvkorting {
  avkortingGrunnlag: IAvkortingGrunnlagFrontend[]
  avkortetYtelse: IAvkortetYtelse[]
  tidligereAvkortetYtelse: IAvkortetYtelse[]
}

export interface IAvkortingSkalHaToInntekt {
  skalHaToInntekter: boolean
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
  inntektTom: number
  fratrekkInnAar: number
  innvilgaMaaneder: number
  inntektUtlandTom: number
  fratrekkInnAarUtland: number
  spesifikasjon: string
  overstyrtInnvilgaMaaneder?: IOverstyrtInnvilgaMaaneder
  kilde: {
    tidspunkt: string
    ident: string
  }
}

export interface IAvkortingGrunnlagLagre {
  id?: string
  inntektTom?: number
  fratrekkInnAar?: number
  inntektUtlandTom?: number
  fratrekkInnAarUtland?: number
  spesifikasjon: string
  fom?: string
  overstyrtInnvilgaMaaneder?: IOverstyrtInnvilgaMaaneder
}

export interface IOverstyrtInnvilgaMaaneder {
  antall: number
  aarsak: OverstyrtInnvilgaMaanederAarsak | SystemOverstyrtInnvilgaMaanederAarsak
  begrunnelse: string
}

export enum OverstyrtInnvilgaMaanederAarsak {
  TAR_UT_PENSJON_TIDLIG = 'TAR_UT_PENSJON_TIDLIG',
  ANNEN = 'ANNEN',
}

export enum SystemOverstyrtInnvilgaMaanederAarsak {
  BLIR_67 = 'BLIR_67',
}

export function hentLesbarTekstForInnvilgaMaanederType(
  type: OverstyrtInnvilgaMaanederAarsak | SystemOverstyrtInnvilgaMaanederAarsak
) {
  switch (type) {
    case OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG:
      return 'Tar ut pensjon tidlig'
    case OverstyrtInnvilgaMaanederAarsak.ANNEN:
      return 'Annen'
    case SystemOverstyrtInnvilgaMaanederAarsak.BLIR_67:
      return 'Blir 67'
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
