export interface Journalpost {
  journalpostId: string
  tittel: string
  tema: string
  journalposttype: string
  journalstatus: string
  dokumenter: DokumentInfo[]
  bruker: Bruker
  avsenderMottaker: AvsenderMottaker
  kanal: string
  sak?: JournalpostSak
  datoOpprettet: string
}

export interface OppdaterJournalpostRequest {
  journalpostId: string
  tittel?: string
  tema?: string
  dokumenter?: DokumentInfo[]
  bruker?: Bruker
  avsenderMottaker?: AvsenderMottaker
  sak?: JournalpostSak
}

export interface KnyttTilAnnenSakRequest {
  bruker: {
    id: string
    idType: string
  }
  fagsakId: string
  fagsaksystem: string
  journalfoerendeEnhet: string
  sakstype: string
  tema: string
}

export interface KnyttTilAnnenSakResponse {
  nyJournalpostId: string
}

export interface DokumentInfo {
  dokumentInfoId: string
  tittel: string
  dokumentvarianter: DokumentVariant[]
}

export interface DokumentVariant {
  filtype?: string
  fysiskDokument?: string
  variantformat?: string
}

export interface Bruker {
  id?: string
  type?: BrukerIdType
}

export enum BrukerIdType {
  ORGNR = 'ORGNR',
  AKTOERID = 'AKTOERID',
  FNR = 'FNR',
}

export interface AvsenderMottaker {
  id?: string
  idType?: string
  navn?: string
  land?: string
}

export interface JournalpostSak {
  sakstype?: Sakstype
  fagsakId?: string
  fagsaksystem?: string
  tema?: string
}

export enum Sakstype {
  FAGSAK = 'FAGSAK',
  GENERELL_SAK = 'GENERELL_SAK',
}
