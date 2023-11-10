export interface Generellbehandling {
  id: string
  sakId: number
  innhold: Innhold | null
  type: GenerellBehandlingType
  opprettet: Date
  status: Status
  tilknyttetBehandling: string
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
  dokumenter?: DokumentSendtMedDato[]
  rinanummer?: string
  begrunnelse?: string
}

export interface DokumentSendtMedDato {
  dokumenttype: string
  sendt: boolean
  dato?: string
}

export interface Annen {
  type: GenerellBehandlingAnnenType
  innhold: string
}
