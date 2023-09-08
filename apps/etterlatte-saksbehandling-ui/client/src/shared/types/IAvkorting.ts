export interface IAvkorting {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlag[]
  avkortetYtelse: IAvkortetYtelse[]
  tidligereAvkortetYtelse: IAvkortetYtelse[]
}

export interface IAvkortingGrunnlag {
  id?: string
  fom?: string
  tom?: string
  aarsinntekt?: number
  fratrekkInnAar?: number
  relevanteMaanederInnAar?: number
  inntektUtland?: number
  fratrekkInnAarUtland?: number
  spesifikasjon?: string
  kilde?: {
    tidspunkt: ''
    ident: ''
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
}
