import {
  IPar,
  behandlingTypeFilter,
  BehandlingTypeFilter,
  saksbehandlerFilter,
  SaksbehandlerFilter,
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
  behandlingType: IOppgaveFelt
  soeknadType: IOppgaveFelt
  fristdato: IOppgaveFelt
  fnr: IOppgaveFelt
  beskrivelse: IOppgaveFelt
  status: IOppgaveFelt
  saksbehandler: IOppgaveFelt
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

export const initialOppgaveFelter = (saksbehandlerNavn: string): IOppgaveFelter => {
  return {
    regdato: {
      noekkel: 'regdato',
      label: 'Reg. dato',
      filter: {
        type: 'dato',
        selectedValue: '',
      },
    },
    behandlingType: {
      noekkel: 'behandlingType',
      label: 'Behandlingstype',
      filter: {
        type: 'select',
        selectedValue: BehandlingTypeFilter.VELG,
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
    beskrivelse: {
      noekkel: 'beskrivelse',
      label: 'Beskrivelse',
    },
    status: {
      noekkel: 'status',
      label: 'Status',
      filter: {
        type: 'select',
        selectedValue: StatusFilter.VELG,
        nedtrekksliste: statusFilter,
      },
    },
    saksbehandler: {
      noekkel: 'saksbehandler',
      label: 'Saksbehandler',
      filter: {
        type: 'select',
        selectedValue: SaksbehandlerFilter.ALLE,
        nedtrekksliste: saksbehandlerFilter(saksbehandlerNavn),
      },
    },
  }
}
