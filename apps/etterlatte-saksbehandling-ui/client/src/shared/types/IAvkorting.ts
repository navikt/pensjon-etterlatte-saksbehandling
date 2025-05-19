import { SanksjonType } from '~shared/types/sanksjon'

export interface IAvkorting {
  redigerbarForventetInntekt: IAvkortingGrunnlag | undefined // Holder med id?
  redigerbarForventetInntektNesteAar: IAvkortingGrunnlag | undefined
  avkortingGrunnlag: IAvkortingGrunnlag[]
  avkortetYtelse: IAvkortetYtelse[]
  tidligereAvkortetYtelse: IAvkortetYtelse[]
}

export interface IAvkortingSkalHaInntektNesteAar {
  skalHaInntektNesteAar: boolean
}

export type IAvkortingGrunnlag = ForventetInntektGrunnlag | FaktiskInntektGrunnlag

export interface ForventetInntektGrunnlag {
  type: 'FORVENTET_INNTEKT'
  id: string
  fom: string
  tom?: string
  inntektTom: number
  fratrekkInnAar: number
  innvilgaMaaneder: number
  inntektUtlandTom: number
  fratrekkInnAarUtland: number
  inntektInnvilgetPeriode: number
  spesifikasjon: string
  overstyrtInnvilgaMaaneder?: IOverstyrtInnvilgaMaaneder
  kilde: {
    tidspunkt: string
    ident: string
  }
}

export interface FaktiskInntektGrunnlag {
  type: 'FAKTISK_INNTEKT'
  id: string
  fom: string
  tom?: string
  loennsinntekt: number
  naeringsinntekt: number
  afp: number
  utlandsinntekt: number
  inntektInnvilgetPeriode: number
  fratrekkInnAarUtland: number
  innvilgaMaaneder: number
  spesifikasjon: string
  kilde: {
    tidspunkt: string
    ident: string
  }
}

export function erForventetInntekt(
  avkortingGrunnlag: IAvkortingGrunnlag
): avkortingGrunnlag is ForventetInntektGrunnlag {
  return avkortingGrunnlag.type === 'FORVENTET_INNTEKT'
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
