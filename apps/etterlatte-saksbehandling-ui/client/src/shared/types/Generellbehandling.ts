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
  landIsoKode?: string[]
  dokumenter?: Dokumenter
  rinanummer?: string
  begrunnelse?: string
  tilknyttetBehandling?: string
}

export interface Dokumenter {
  p2100: boolean
  p5000: boolean
  p3000: boolean
  p6000: boolean
  p4000: boolean
}

export interface Annen {
  type: GenerellBehandlingAnnenType
  innhold: string
}
