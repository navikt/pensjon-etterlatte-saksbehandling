export interface IAvkorting {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlag[]
  tidspunktForAvkorting: string
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
