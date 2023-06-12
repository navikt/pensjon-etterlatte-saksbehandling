export interface IAvkorting {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlag[]
  avkortetYtelse: IAvkortetYtelse[]
}

export interface IAvkortingGrunnlag {
  id?: string
  fom?: string
  tom?: string
  aarsinntekt?: number
  fratrekkInnAar?: number
  relevanteMaanederInnAar?: number
  spesifikasjon?: string
  kilde?: {
    tidspunkt: ''
    ident: ''
  }
}

export interface IAvkortetYtelse {
  fom: string
  tom: string
  avkortingsbeloep: number
  ytelseEtterAvkorting: number
}
