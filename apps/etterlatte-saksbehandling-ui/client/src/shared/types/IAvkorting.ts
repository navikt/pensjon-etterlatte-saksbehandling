export interface IAvkorting {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlag[]
  avkortetYtelse: IAvkortetYtelse[]
}

export interface IAvkortingGrunnlag {
  fom?: string
  tom?: string
  aarsinntekt?: number
  gjeldendeAar?: number
  spesifikasjon?: string
  kilde?: {
    tidspunkt: ''
    ident: ''
  }
}

export interface IAvkortetYtelse {
  fom: string
  tom: string
  ytelseEtterAvkorting: number
}
