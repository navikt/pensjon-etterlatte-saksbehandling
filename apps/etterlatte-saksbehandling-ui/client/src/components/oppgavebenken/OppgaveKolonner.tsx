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
    Header: 'Reg. dato',
    accessor: 'regdato',
    Cell: ({ value: dato }) => {
      return moment(dato).format()
    },
    sortType: 'datetime',
  },
  {
    Header: 'Frist',
    accessor: 'fristdato',
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
    regdato: new Date(2022, 1, 6),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    regdato: new Date(2020, 3, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    regdato: new Date(2020, 3, 23),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.LAV,
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2020, 3, 22),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2020, 3, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2022, 3, 16),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    regdato: new Date(2020, 3, 15),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    regdato: new Date(2020, 3, 14),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.LAV,
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2020, 3, 13),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2020, 3, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2020, 6, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    regdato: new Date(2020, 5, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.HOEY,
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    enhet: EnhetFilter.E4820,
  },
  {
    regdato: new Date(2020, 4, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.LAV,
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2021, 3, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    enhet: EnhetFilter.E4806,
  },
  {
    regdato: new Date(2022, 3, 12),
    fristdato: new Date(2022, 1, 6),
    prioritet: PrioritetFilter.NORMAL,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    enhet: EnhetFilter.E4806,
  },
]
