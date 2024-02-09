export interface ISaksbehandler {
  ident: string
  navn: string
  enheter: Array<string>
  kanAttestere: boolean
  leseTilgang: boolean
  skriveTilgang: boolean
}
