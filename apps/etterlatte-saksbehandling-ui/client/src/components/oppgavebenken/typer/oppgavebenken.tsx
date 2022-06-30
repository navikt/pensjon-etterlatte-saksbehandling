export interface IOppgave {
  sakId: number
  behandlingsId: string
  regdato: Date
  fristdato: Date
  fnr: string
  soeknadType: SoeknadTypeFilter
  behandlingType: BehandlingTypeFilter
  beskrivelse: string
  status: StatusFilter
  saksbehandler: string
  handling: Handlinger
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
  GYLDIG_SOEKNAD = 'GYLDIG_SOEKNAD',
  IKKE_GYLDIG_SOEKNAD = 'IKKE_GYLDIG_SOEKNAD',
  UNDER_BEHANDLING = 'UNDER_BEHANDLING',
  FATTET_VEDTAK = 'FATTET_VEDTAK',
  RETURNERT = 'RETURNERT',
  ATTESTERT = 'ATTESTERT',
  IVERKSATT = 'IVERKSATT',
  AVBRUTT = 'AVBRUTT',
}

export const statusFilter: Record<StatusFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  GYLDIG_SOEKNAD: { id: 'GYLDIG_SOEKNAD', navn: 'Gyldig søknad' },
  IKKE_GYLDIG_SOEKNAD: { id: 'IKKE_GYLDIG_SOEKNAD', navn: 'Ikke gyldig søknad' },
  UNDER_BEHANDLING: { id: 'UNDER_BEHANDLING', navn: 'Under behandling' },
  FATTET_VEDTAK: { id: 'FATTET_VEDTAK', navn: 'Fattet vedtak' },
  RETURNERT: { id: 'RETURNERT', navn: 'Returnert' },
  ATTESTERT: { id: 'ATTESTERT', navn: 'Attestert' },
  IVERKSATT: { id: 'IVERKSATT', navn: 'Iverksatt' },
  AVBRUTT: { id: 'AVBRUTT', navn: 'Avbrutt' },
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
