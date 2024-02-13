export interface Saksbehandler {
  ident: string
  navn: string
}

export interface SaksbehandlerMedInformasjon extends Saksbehandler {
  enheter: Array<string>
  kanAttestere: boolean
  leseTilgang: boolean
  skriveTilgang: boolean
}
