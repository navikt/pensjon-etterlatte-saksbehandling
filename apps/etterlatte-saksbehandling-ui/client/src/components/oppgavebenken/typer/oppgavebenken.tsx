export interface IOppgave {
  sakId: number
  behandlingId?: string
  regdato: Date
  fristdato: Date
  fnr: string
  soeknadType: SoeknadTypeFilter
  oppgaveType: OppgaveTypeFilter
  oppgaveStatus: StatusFilter
  saksbehandler: string
  handling: Handlinger
  merknad?: string
  oppgaveEnhet: EnhetFilter
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

export enum EnhetFilter {
  VELG = 'VELG',
  INGEN_ENHET = 'INGEN_ENHET',
  E0001 = 'E0001',
  E2103 = 'E2103',
  E4808 = 'E4808',
  E4815 = 'E4815',
  E4817 = 'E4817',
  E4862 = 'E4862',
  E4883 = 'E4883',
}

export const enhetFilter: Record<EnhetFilter, IPar> = {
  VELG: { id: 'VELG', navn: 'Velg' },

  INGEN_ENHET: { id: 'INGEN_ENHET', navn: 'Ingen' },
  E4815: { id: 'E4815', navn: '4815 - Ålesund' },
  E4808: { id: 'E4808', navn: '4808 - Porsgrunn' },
  E4817: { id: 'E4817', navn: '4817 - Steinkjer' },
  E4862: { id: 'E4862', navn: '4862 - Ålesund Utland' },
  E0001: { id: 'E0001', navn: '0001 - Utland' },
  E4883: { id: 'E4883', navn: '4883 - Egne ansatte' },
  E2103: { id: 'E2103', navn: '2103 - Vikafossen' },
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
