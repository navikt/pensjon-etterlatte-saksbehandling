export interface IOppgave {
  sakId: number
  behandlingId?: string
  regdato: Date
  fristdato: Date
  fnr: string
  soeknadType: SoeknadTypeFilter
  oppgaveType: OppgaveTypeFilter
  beskrivelse: string
  oppgaveStatus: StatusFilter
  saksbehandler: string
  handling: Handlinger
  merknad: string
}

export enum Handlinger {
  BEHANDLE = 'BEHANDLE',
  GAA_TIL_SAK = 'GAA_TIL_SAK',
}

export const handlinger: Record<Handlinger, IPar> = {
  BEHANDLE: { id: 'BEHANDLE', navn: 'Start behandling' },
  GAA_TIL_SAK: { id: 'GAA_TIL_SAK', navn: 'Gå til sak' },
}

export enum OppgaveTypeFilter {
  VELG = 'VELG',
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
  ENDRING_PAA_SAK = 'ENDRING_PAA_SAK',
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
}

export const oppgaveTypeFilter: Record<OppgaveTypeFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  FØRSTEGANGSBEHANDLING: { id: 'FØRSTEGANGSBEHANDLING', navn: 'Førstegangsbehandling' },
  REVURDERING: { id: 'REVURDERING', navn: 'Revurdering' },
  MANUELT_OPPHOER: { id: 'MANUELT_OPPHOER', navn: 'Manuelt opphør' },
  ENDRING_PAA_SAK: { id: 'ENDRING_PAA_SAK', navn: 'Endring på sak' },
}

export enum SoeknadTypeFilter {
  VELG = 'VELG',
  BARNEPENSJON = 'BARNEPENSJON',
  OMSTILLINGSSTOENAD = 'OMSTILLINGSSTOENAD',
}

export const soeknadTypeFilter: Record<SoeknadTypeFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },
  BARNEPENSJON: { id: 'BARNEPENSJON', navn: 'Barnepensjon' },
  OMSTILLINGSSTOENAD: { id: 'OMSTILLINGSSTOENAD', navn: 'Omstillingsstønad' },
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
