export interface IOppgave {
  sakId: number
  behandlingsId: string
  regdato: Date
  fristdato: Date
  fnr: string
  soeknadType: SoeknadTypeFilter
  behandlingType: BehandlingTypeFilter
  beskrivelse: string
  oppgaveStatus: StatusFilter
  saksbehandler: string
  handling: Handlinger
  antallSoesken: number | null
}

export enum Handlinger {
  BEHANDLE = 'BEHANDLE',
}

export const handlinger: Record<Handlinger, IPar> = {
  BEHANDLE: { id: 'BEHANDLE', navn: 'Start behandling' },
}

export enum BehandlingTypeFilter {
  VELG = 'VELG',
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
}

export const behandlingTypeFilter: Record<BehandlingTypeFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  FØRSTEGANGSBEHANDLING: { id: 'FØRSTEGANGSBEHANDLING', navn: 'Førstegangsbehandling' },
  REVURDERING: { id: 'REVURDERING', navn: 'Revurdering' },
}

export enum SoeknadTypeFilter {
  VELG = 'VELG',
  BARNEPENSJON = 'BARNEPENSJON',
  GJENLEVENDEPENSJON = 'GJENLEVENDEPENSJON',
}

export const soeknadTypeFilter: Record<SoeknadTypeFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  BARNEPENSJON: { id: 'BARNEPENSJON', navn: 'Barnepensjon' },
  GJENLEVENDEPENSJON: { id: 'GJENLEVENDEPENSJON', navn: 'Gjenlevendepensjon' },
}

export enum StatusFilter {
  VELG = 'VELG',
  NY = 'NY',
  TIL_ATTESTERING = 'TIL_ATTESTERING',
  RETURNERT = 'RETURNERT',
}

export const statusFilter: Record<StatusFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  NY: { id: 'NY', navn: 'Ny' },
  TIL_ATTESTERING: { id: 'TIL_ATTESTERING', navn: 'Til attestering' },
  RETURNERT: { id: 'RETURNERT', navn: 'Returnert' },
}

export enum SaksbehandlerFilter {
  INNLOGGET = 'INNLOGGET',
  ALLE = 'ALLE',
  FORDELTE = 'FORDELTE',
  UFORDELTE = 'UFORDELTE',
}

export const saksbehandlerFilter = (innloggetSaksbehandler: string): Record<SaksbehandlerFilter, IPar> => {
  return {
    INNLOGGET: { id: 'INNLOGGET', navn: innloggetSaksbehandler },
    ALLE: { id: 'ALLE', navn: 'Alle' },
    FORDELTE: { id: 'FORDELTE', navn: 'Fordelte' },
    UFORDELTE: { id: 'UFORDELTE', navn: 'Ufordelte' },
  }
}

export interface SelectFilter {
  filter: StatusFilter
}

export interface FilterPar {
  id: string
  value: string | undefined
}

export interface IPar {
  id: string
  navn: string
}

export interface INoekkelPar {
  [key: string]: IPar
}
