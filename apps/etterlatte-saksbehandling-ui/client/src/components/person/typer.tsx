export interface Behandling {
  id: number
  opprettet: string
  type: string
  årsak: string
  status: string
  vedtaksdato: string
  resultat: string
}
export interface Sak {
  sakId: number
  type: string
  sakstype: string
  behandlinger: Behandling[]
}
export interface SakslisteProps {
  saker: Sak[]
}

export interface Dokument {
  dato: string
  tittel: string
  link: string
  status: string
}

export interface Dokumenter {
  brev: Dokument[]
}
