export interface Generellbehandling {
  id: string
  sakId: number
  innhold: Innhold
  type: GenerellBehandlingType
  opprettet: Date
  status: Status
  tilknyttetBehandling?: string
}

export enum Status {
  OPPRETTET = 'OPPRETTET',
  FATTET = 'FATTET',
  ATTESTERT = 'ATTESTERT',
  AVBRUTT = 'AVBRUTT',
}

export type GenerellBehandlingType = GenerellBehandlingKravpakkeUtlandType | GenerellBehandlingAnnenType

type GenerellBehandlingKravpakkeUtlandType = 'KRAVPAKKE_UTLAND'
type GenerellBehandlingAnnenType = 'ANNEN'

export type Innhold = KravpakkeUtland | Annen

export interface KravpakkeUtland {
  type: GenerellBehandlingKravpakkeUtlandType
  landIsoKode?: string[]
  dokumenter?: Dokumenter
  rinanummer?: string
  begrunnelse?: string
}

export interface Dokumenter {
  p2100: DokumentSendtMedDato
  p5000: DokumentSendtMedDato
  p3000: DokumentSendtMedDato
  p6000: DokumentSendtMedDato
  p4000: DokumentSendtMedDato
}

export interface DokumentSendtMedDato {
  sendt: boolean
  dato?: string
}

export interface Annen {
  type: GenerellBehandlingAnnenType
  innhold: string
}
