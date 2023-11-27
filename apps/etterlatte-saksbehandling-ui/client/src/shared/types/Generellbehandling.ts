interface Behandler {
  saksbehandler: string
  tidspunkt: string
}

interface Attestant {
  attestant: string
  tidspunkt: string
}

export interface Generellbehandling {
  id: string
  sakId: number
  innhold: Innhold | null
  type: GenerellBehandlingType
  opprettet: string
  status: Status
  tilknyttetBehandling: string
  behandler?: Behandler
  attestant?: Attestant
  kommentar?: string
}

export function generellbehandlingErRedigerbar(status: Status): boolean {
  switch (status) {
    case Status.RETURNERT:
    case Status.OPPRETTET:
      return true
    case Status.FATTET:
    case Status.ATTESTERT:
    case Status.AVBRUTT:
      return false
  }
}

export enum Status {
  OPPRETTET = 'OPPRETTET',
  FATTET = 'FATTET',
  RETURNERT = 'RETURNERT',
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
