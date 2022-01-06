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
import moment from 'moment'
moment.defaultFormat = 'DD.MM.YYYY'

export const kolonner: ReadonlyArray<Column<IOppgave>> = [
  {
    Header: 'ID',
    accessor: 'id',
  },
  {
    Header: 'Reg. dato',
    accessor: 'regdato',
    Cell: ({ value: dato }) => {
      return moment(dato).format()
    },
    sortType: 'datetime',
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
    regdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '2',
    regdato: new Date(2020, 3, 12),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '3',
    regdato: new Date(2020, 3, 23),
    prioritet: PrioritetFilter.LAV,
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '4',
    regdato: new Date(2020, 3, 22),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '5',
    regdato: new Date(2020, 3, 12),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '6',
    regdato: new Date(2022, 3, 16),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '7',
    regdato: new Date(2020, 3, 15),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '8',
    regdato: new Date(2020, 3, 14),
    prioritet: PrioritetFilter.LAV,
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '9',
    regdato: new Date(2020, 3, 13),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '10',
    regdato: new Date(2020, 3, 12),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '11',
    regdato: new Date(2020, 6, 12),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '12',
    regdato: new Date(2020, 5, 12),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    id: '13',
    regdato: new Date(2020, 4, 12),
    prioritet: PrioritetFilter.LAV,
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '14',
    regdato: new Date(2021, 3, 12),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    id: '15',
    regdato: new Date(2022, 3, 12),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    enhet: EnhetFilter.E4806,
  },
]
