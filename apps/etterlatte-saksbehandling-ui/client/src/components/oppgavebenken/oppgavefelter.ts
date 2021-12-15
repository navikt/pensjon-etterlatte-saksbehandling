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
  initialValue?: string
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
        initialValue: '',
        selectedValue: '',
      },
    },
    prioritet: {
      noekkel: 'prioritet',
      label: 'Prioritet',
      filter: {
        type: 'select',
        initialValue: PrioritetFilter.VELG,
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
        initialValue: StatusFilter.VELG,
        selectedValue: StatusFilter.VELG,
        nedtrekksliste: statusFilter,
      },
    },
    enhet: {
      noekkel: 'enhet',
      label: 'Enhet',
      filter: {
        type: 'select',
        initialValue: EnhetFilter.VELG,
        selectedValue: EnhetFilter.VELG,
        nedtrekksliste: enhetFilter,
      },
    },
  }
}
