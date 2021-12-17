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
  selectedValue: string
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
  id: IOppgaveFelt
  regdato: IOppgaveFelt
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
    id: {
      noekkel: 'id',
      label: 'ID initiell test',
    },
    regdato: {
      noekkel: 'regdato',
      label: 'Reg. dato',
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
