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
  inntektTom: number
  fratrekkInnAar: number
  innvilgaMaaneder: number
  inntektUtlandTom: number
  fratrekkInnAarUtland: number
  spesifikasjon: string
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
  aarsak: OverstyrtInnvilaMaanederAarsak
  begrunnelse: string
}

export enum OverstyrtInnvilaMaanederAarsak {
  TAR_UT_PENSJON_TIDLIG = 'TAR_UT_PENSJON_TIDLIG',
  BLIR_67 = 'BLIR_67',
  ANNEN = 'ANNEN',
}

export function hentLesbarTekstForInnvilgaMaanederType(type: OverstyrtInnvilaMaanederAarsak) {
  switch (type) {
    case OverstyrtInnvilaMaanederAarsak.TAR_UT_PENSJON_TIDLIG:
      return 'Tar ut pensjon tidlig'
    case OverstyrtInnvilaMaanederAarsak.BLIR_67:
      return 'Blir 67'
    case OverstyrtInnvilaMaanederAarsak.ANNEN:
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
