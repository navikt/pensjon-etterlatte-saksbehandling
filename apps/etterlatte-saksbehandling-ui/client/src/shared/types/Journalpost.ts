export interface Journalposter {
  journalposter: Journalpost[]
  sideInfo: SideInfo
}

export interface Journalpost {
  journalpostId: string
  tittel: string
  tema: string
  journalposttype: Journalposttype
  journalstatus: Journalstatus
  dokumenter: DokumentInfo[]
  bruker: Bruker
  avsenderMottaker: AvsenderMottaker
  kanal: string
  sak?: JournalpostSak
  datoOpprettet: string
  utsendingsinfo?: object
  relevanteDatoer: RelevantDato[]
}

export interface SideInfo {
  sluttpeker: string
  finnesNesteSide: boolean
  antall: number
  totaltAntall: number
}

export interface JournalpostUtsendingsinfo {
  journalpostId: string
  utsendingsinfo?: {
    fysiskpostSendt?: {
      adressetekstKonvolutt?: string
    }
    digitalpostSendt?: {
      adresse?: string
    }
  }
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
  saksbehandlerHarTilgang?: boolean
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

export enum AvsenderMottakerIdType {
  ORGNR = 'ORGNR',
  AKTOERID = 'AKTOERID',
  FNR = 'FNR',
  UTL_ORG = 'UTL_ORG',
  HPRNR = 'HPRNR',
}

export interface AvsenderMottaker {
  id?: string
  idType?: AvsenderMottakerIdType
  navn?: string
  land?: string
}

export interface SoekPersonValg {
  id?: string
  navn?: string
  type?: BrukerIdType
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

export enum Journalstatus {
  MOTTATT = 'MOTTATT',
  JOURNALFOERT = 'JOURNALFOERT',
  FERDIGSTILT = 'FERDIGSTILT',
  EKSPEDERT = 'EKSPEDERT',
  UNDER_ARBEID = 'UNDER_ARBEID',
  FEILREGISTRERT = 'FEILREGISTRERT',
  UTGAAR = 'UTGAAR',
  AVBRUTT = 'AVBRUTT',
  UKJENT_BRUKER = 'UKJENT_BRUKER',
  RESERVERT = 'RESERVERT',
  OPPLASTING_DOKUMENT = 'OPPLASTING_DOKUMENT',
  UKJENT = 'UKJENT',
}

export enum Journalposttype {
  I = 'I', // Inngående
  U = 'U', // Utgående
  N = 'N', // Notat
}

export enum Tema {
  EYB = 'EYB',
  EYO = 'EYO',
  PEN = 'PEN',
}

export interface RelevantDato {
  datotype: Datotype
  dato: string
}

export enum Datotype {
  DATO_SENDT_PRINT = 'Dato sendt til print',
  DATO_EKSPEDERT = 'Dato sendt til bruker',
  DATO_JOURNALFOERT = 'Dato journalført',
  DATO_REGISTRERT = 'Dato registrert',
  DATO_AVS_RETUR = 'Dato sendt på nytt',
  DATO_DOKUMENT = 'Dato på hoveddokumentet',
  DATO_LEST = 'Dato åpnet på nav.no',
}
