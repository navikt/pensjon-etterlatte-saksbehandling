export interface ISaksbehandler {
  ident: string
  navn: string
  fornavn: string
  etternavn: string
  enheter: IEnhet[]
  kanAttestere: boolean
  leseTilgang: boolean
  skriveTilgang: boolean
}

interface IEnhet {
  enhetId: string
  navn: string
}
