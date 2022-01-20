export interface IOppgave {
  regdato: Date
  soeknadstype: SoeknadstypeFilter
  oppgavetype: OppgavetypeFilter
  fristdato: Date
  prioritet: PrioritetFilter
  bruker: string //todo endre til fnr senere
  beskrivelse: string
  status: StatusFilter
  enhet: EnhetFilter
  saksbehandler: string
  handlinger: Handlinger
}

export enum Handlinger {
  START = 'Behandle sak',
}

export enum PrioritetFilter {
  VELG = 'VELG',
  LAV = 'LAV',
  NORMAL = 'NORMAL',
  HOEY = 'HOEY',
}

export const prioritetFilter: Record<PrioritetFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  LAV: { id: 'LAV', navn: 'Lav' },
  NORMAL: { id: 'NORMAL', navn: 'Normal' },
  HOEY: { id: 'HOEY', navn: 'Høy' },
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

export enum EnhetFilter {
  VELG = 'VELG',
  E4806 = 'E4806',
  E4820 = 'E4820',
  E4833 = 'E4833',
  E4842 = 'E4842',
  E4817 = 'E4817',
}

export const enhetFilter: Record<EnhetFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  E4806: { id: 'E4806', navn: '4806 Drammen' },
  E4820: { id: 'E4820', navn: '4820 Vadsø' },
  E4833: { id: 'E4833', navn: '4833 Oslo' },
  E4842: { id: 'E4842', navn: '4842 Stord' },
  E4817: { id: 'E4817', navn: '4817 Steinkjer' },
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
  filter: EnhetFilter | StatusFilter | PrioritetFilter
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
