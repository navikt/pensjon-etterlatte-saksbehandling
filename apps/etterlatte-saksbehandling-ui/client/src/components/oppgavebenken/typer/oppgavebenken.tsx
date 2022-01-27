export interface IOppgave {
  sakId: number
  behandlingsId: string
  regdato: Date
  soeknadType: SoeknadTypeFilter
  behandlingType: BehandlingTypeFilter
  fristdato: Date
  fnr: string
  beskrivelse: string
  status: StatusFilter
  saksbehandler: string
  handling: Handlinger
}

export enum Handlinger {
  BEHANDLE = 'Behandle sak',
}

export enum BehandlingTypeFilter {
  VELG = 'VELG',
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING',
}

export const behandlingTypeFilter: Record<BehandlingTypeFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  FØRSTEGANGSBEHANDLING: { id: 'FØRSTEGANGSBEHANDLING', navn: 'Førstegangsbehandling' },
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
  OPPRETTET = 'OPPRETTET',
  UNDER_BEHANDLING = 'UNDER_BEHANDLING',
  FERDIG = 'FERDIG',
}

export const statusFilter: Record<StatusFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  NY: { id: 'NY', navn: 'Ny' },
  OPPRETTET: { id: 'OPPRETTET', navn: 'Opprettet' },
  UNDER_BEHANDLING: { id: 'UNDER_BEHANDLING', navn: 'Under behandling' },
  FERDIG: { id: 'FERDIG', navn: 'Ferdig' },
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
