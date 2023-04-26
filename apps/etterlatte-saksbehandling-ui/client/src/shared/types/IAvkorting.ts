export interface IAvkorting {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlag[]
  tidspunktForAvkorting: string
}

export interface IAvkortingGrunnlag {
  fom?: string
  tom?: string
  aarsInntekt?: number
  gjeldendeAar?: number
  spesifikasjon?: string
}
