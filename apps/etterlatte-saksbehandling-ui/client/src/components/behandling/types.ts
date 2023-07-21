export interface Journalpost {
  journalpostId: string
  tittel: string
  journalposttype: string
  journalstatus: string
  dokumenter: Dokument[]
  avsenderMottaker: {
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
