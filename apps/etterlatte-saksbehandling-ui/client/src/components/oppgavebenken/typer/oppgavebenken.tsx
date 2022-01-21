export interface IOppgave {
  regdato: Date
  soeknadstype: SoeknadstypeFilter
  oppgavetype: OppgavetypeFilter
  fristdato: Date
  bruker: string
  beskrivelse: string
  status: StatusFilter
  saksbehandler: string
  handlinger: Handlinger
}

export enum Handlinger {
  START = 'Behandle sak',
}

export enum OppgavetypeFilter {
  VELG = 'VELG',
  FOERSTEGANGSBEHANDLING = 'FOERSTEGANGSBEHANDLING',
}

export const oppgavetypeFilter: Record<OppgavetypeFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  FOERSTEGANGSBEHANDLING: { id: 'FOERSTEGANGSBEHANDLING', navn: 'Førstegangsbehandling' },
}

export enum SoeknadstypeFilter {
  VELG = 'VELG',
  BARNEPENSJON = 'BARNEPENSJON',
  GJENLEVENDEPENSJON = 'GJENLEVENDEPENSJON',
}

export const soeknadstypeFilter: Record<SoeknadstypeFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  BARNEPENSJON: { id: 'BARNEPENSJON', navn: 'Barnepensjon' },
  GJENLEVENDEPENSJON: { id: 'GJENLEVENDEPENSJON', navn: 'Gjenlevendepensjon' },
}

export enum StatusFilter {
  VELG = 'VELG',
  NY = 'NY',
  UNDER_BEHANDLING = 'UNDER_BEHANDLING',
  FERDIG = 'FERDIG',
}

export const statusFilter: Record<StatusFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  NY: { id: 'NY', navn: 'Ny' },
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
