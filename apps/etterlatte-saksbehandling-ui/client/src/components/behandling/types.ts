export enum RelatertPersonsRolle {
  BARN = 'barn',
  FORELDER = 'forelder',
  SOESKEN = 'søsken',
}

export enum PersonStatus {
  AVDOED = 'Avdød',
  GJENLEVENDE_FORELDER = 'Gjenlevende',
  BARN = 'Etterlatt',
  ETTERLATT = 'Etterlatt',
}

export interface Journalpost {
  journalpostId: string
  tittel: string
  journalposttype: string
  journalstatus: string
  dokumenter: Dokument[]
  avsenderMottaker: {
    id: string
    navn: string
    erLikBruker: boolean
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
