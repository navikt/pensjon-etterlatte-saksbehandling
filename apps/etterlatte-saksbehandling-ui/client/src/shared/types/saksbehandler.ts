export interface Saksbehandler {
  ident: string
  navn: string
}

export interface InnloggetSaksbehandler extends Saksbehandler {
  enheter: Array<string>
  kanAttestere: boolean
  skriveEnheter: Array<string>
  kanSeOppgaveliste: boolean
}
