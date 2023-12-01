export interface ISaksbehandler {
  ident: string
  navn: string
  fornavn: string
  etternavn: string
  enheter: IEnhet[]
  kanAttestere: boolean
}

interface IEnhet {
  enhetId: string
  navn: string
}
