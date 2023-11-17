import { ISak } from '~shared/types/sak'

export interface Journalpost {
  journalpostId: string
  tittel: string
  tema: string
  journalposttype: string
  journalstatus: string
  dokumenter: Dokument[]
  avsenderMottaker: {
    id?: string
    navn?: string
  }
  kanal: string
  datoOpprettet: string
}

interface Dokument {
  dokumentInfoId: string
  tittel: string
  dokumentvarianter: {
    saksbehandlerHarTilgang: boolean
  }[]
}

export enum JournalpostVariant {
  NY_SOEKNAD = 'NY_SOEKNAD',
  NYTT_VEDLEGG = 'NYTT_VEDLEGG',
  FEIL_TEMA = 'FEIL_TEMA',
}

export interface OppdaterJournalpostTemaRequest {
  nyttTema?: string
}

export interface FerdigstillJournalpostRequest {
  sak?: ISak
}
