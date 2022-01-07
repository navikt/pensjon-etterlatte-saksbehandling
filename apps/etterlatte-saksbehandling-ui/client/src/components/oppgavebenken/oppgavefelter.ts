import {
  enhetFilter,
  EnhetFilter,
  IPar,
  prioritetFilter,
  PrioritetFilter,
  statusFilter,
  StatusFilter,
} from '../../typer/oppgavebenken'

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
  prioritet: IOppgaveFelt
  bruker: IOppgaveFelt
  beskrivelse: IOppgaveFelt
  status: IOppgaveFelt
  enhet: IOppgaveFelt
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
    prioritet: {
      noekkel: 'prioritet',
      label: 'Prioritet',
      filter: {
        type: 'select',
        selectedValue: PrioritetFilter.VELG,
        nedtrekksliste: prioritetFilter,
      },
    },
    bruker: {
      noekkel: 'bruker',
      label: 'Bruker',
      filter: {
        type: 'string',
        selectedValue: '',
      },
    },
    beskrivelse: {
      noekkel: 'beskrivelse',
      label: 'Beskrivelse',
      filter: {
        type: 'string',
        selectedValue: '',
      },
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
    enhet: {
      noekkel: 'enhet',
      label: 'Enhet',
      filter: {
        type: 'select',
        selectedValue: EnhetFilter.VELG,
        nedtrekksliste: enhetFilter,
      },
    },
  }
}
