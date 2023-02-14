import {
  behandlingTypeFilter,
  IPar,
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
  behandlingType: IOppgaveFelt
  soeknadType: IOppgaveFelt
  beskrivelse: IOppgaveFelt
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

    behandlingType: {
      noekkel: 'behandlingType',
      label: 'Behandlingstype',
      filter: {
        type: 'select',
        selectedValue: OppgaveTypeFilter.VELG,
        nedtrekksliste: behandlingTypeFilter,
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
    beskrivelse: {
      noekkel: 'beskrivelse',
      label: 'Beskrivelse',
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
