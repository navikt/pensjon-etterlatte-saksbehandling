export interface Generellbehandling {
  id: string
  sakId: number
  innhold: Innhold
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
  sed: string
  tilknyttetBehandling: string
}

export interface Annen {
  type: GenerellBehandlingAnnenType
  innhold: string
}
