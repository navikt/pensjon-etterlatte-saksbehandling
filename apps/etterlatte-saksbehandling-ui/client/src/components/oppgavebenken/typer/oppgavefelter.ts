import {
  IPar,
  oppgaveTypeFilter,
  OppgaveTypeFilter,
  soeknadTypeFilter,
  SoeknadTypeFilter,
  statusFilter,
  StatusFilter,
} from './oppgavebenken'

export interface IOppgaveFilter {
  selectedValue: any
  type: 'dato' | 'select' | 'string'
  nedtrekksliste?: Record<any, IPar>
}

export interface IOppgaveFelt {
  noekkel: string
  label: string
  filter?: IOppgaveFilter
}

export interface IOppgaveFelter {
  [key: string]: IOppgaveFelt

  regdato: IOppgaveFelt
  fristdato: IOppgaveFelt
  fnr: IOppgaveFelt
  oppgaveType: IOppgaveFelt
  soeknadType: IOppgaveFelt
  oppgaveStatus: IOppgaveFelt
}

export enum FeltSortOrder {
  NONE = 'NONE',
  ASCENDANT = 'ASCENDANT',
  DESCENDANT = 'DESCENDANT',
}

export const ariaSortMap = new Map<FeltSortOrder, 'none' | 'descending' | 'ascending'>([
  [FeltSortOrder.NONE, 'none'],
  [FeltSortOrder.DESCENDANT, 'descending'],
  [FeltSortOrder.ASCENDANT, 'ascending'],
])

export const initialOppgaveFelter = (): IOppgaveFelter => {
  return {
    regdato: {
      noekkel: 'regdato',
      label: 'Reg. dato',
      filter: {
        type: 'dato',
        selectedValue: '',
      },
    },
    fristdato: {
      noekkel: 'fristdato',
      label: 'Frist',
      filter: {
        type: 'dato',
        selectedValue: '',
      },
    },
    fnr: {
      noekkel: 'fnr',
      label: 'Fødselsnummer',
      filter: {
        type: 'string',
        selectedValue: '',
      },
    },
    oppgaveType: {
      noekkel: 'oppgaveType',
      label: 'Oppgavetype',
      filter: {
        type: 'select',
        selectedValue: OppgaveTypeFilter.VELG,
        nedtrekksliste: oppgaveTypeFilter,
      },
    },
    soeknadType: {
      noekkel: 'soeknadType',
      label: 'Ytelse',
      filter: {
        type: 'select',
        selectedValue: SoeknadTypeFilter.VELG,
        nedtrekksliste: soeknadTypeFilter,
      },
    },
    oppgaveStatus: {
      noekkel: 'oppgaveStatus',
      label: 'Status',
      filter: {
        type: 'select',
        selectedValue: StatusFilter.VELG,
        nedtrekksliste: statusFilter,
      },
    },
  }
}
