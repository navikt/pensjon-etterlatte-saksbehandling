import { Column } from 'react-table'
import {
  enhetFilter,
  EnhetFilter,
  IOppgave,
  prioritetFilter,
  PrioritetFilter,
  statusFilter,
  StatusFilter,
} from '../../typer/oppgavebenken'

export const kolonner: ReadonlyArray<Column<IOppgave>> = [
  {
    Header: 'ID',
    accessor: 'id',
  },
  {
    Header: 'Reg. dato',
    accessor: 'regdato',
  },
  {
    Header: 'Prioritet',
    accessor: 'prioritet',
    filter: 'exact',
    Cell: ({ value: prioritet }) => {
      return prioritet ? prioritetFilter[prioritet as PrioritetFilter]?.navn ?? prioritet : 'Ukjent'
    },
  },
  {
    Header: 'Bruker',
    accessor: 'bruker',
  },
  {
    Header: 'Beskrivelse',
    accessor: 'beskrivelse',
    disableSortBy: true,
  },
  {
    Header: 'Status',
    accessor: 'status',
    filter: 'exact',
    Cell: ({ value: status }) => {
      return status ? statusFilter[status as StatusFilter]?.navn ?? status : 'Ukjent'
    },
  },
  {
    Header: 'Enhet',
    accessor: 'enhet',
    filter: 'exact',
    Cell: ({ value: enhet }) => {
      return enhet ? enhetFilter[enhet as EnhetFilter]?.navn ?? enhet : 'Ukjent'
    },
  },
]

export const mockdata: ReadonlyArray<IOppgave> = [
  {
    id: '1',
    regdato: '12.03.20',
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '2',
    regdato: '12.03.20',
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '3',
    regdato: '13.03.20',
    prioritet: PrioritetFilter.LAV,
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '4',
    regdato: '14.03.20',
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '5',
    regdato: '15.03.20',
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    enhet: EnhetFilter.E4806,
  },
]
