export type Kilde = KildeSaksbehandler | KildePdl

export enum KildeType {
  saksbehandler = 'saksbehandler',
  privatperson = 'privatperson',
  a_ordningen = 'a-ordningen',
  aa_registeret = 'aa-registeret',
  vilkaarskomponenten = 'vilkaarskomponenten',
  pdl = 'pdl',
}

export interface KildeSaksbehandler {
  type: KildeType.saksbehandler
  tidspunkt: string
  ident: string
}

export interface KildePdl {
  type: KildeType.pdl
  tidspunktForInnhenting: string
  navn: string
  registersReferanse: string
  opplysningId: string
}
