export interface Generellbehandling {
  id: string
  sakId: number
  innhold: Innhold
  type: GenerellBehandlingType
  opprettet: Date
}

export const Innholdstyper: Record<GenerellBehandlingType, string> = {
  UTLAND: 'Utland',
  ANNEN: 'Annen',
}

export type GenerellBehandlingType = GenerellBehandlingUtlandType | GenerellBehandlingAnnenType

type GenerellBehandlingUtlandType = 'UTLAND'
type GenerellBehandlingAnnenType = 'ANNEN'

export type Innhold = Utland | Annen

export interface Utland {
  type: GenerellBehandlingUtlandType
  landIsoKode: string
  dokumenter: Dokumenter
  begrunnelse: string
  tilknyttetBehandling?: string
}

export interface Dokumenter {
  P2100: boolean
  P5000: boolean
  P3000: boolean
}

export interface Annen {
  type: GenerellBehandlingAnnenType
  innhold: string
}
