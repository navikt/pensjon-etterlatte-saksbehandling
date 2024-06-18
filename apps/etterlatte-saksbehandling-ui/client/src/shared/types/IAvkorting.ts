import { SanksjonType } from '~shared/types/sanksjon'

export interface IAvkorting {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlag[]
  avkortetYtelse: IAvkortetYtelse[]
  tidligereAvkortetYtelse: IAvkortetYtelse[]
}

export interface IAvkortingGrunnlag {
  id: string
  fom: string
  tom?: string
  aarsinntekt: number
  fratrekkInnAar: number
  forventaInnvilgaMaaneder: number
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
  type: SanksjonType
}
