import { Column } from 'react-table'
import {
  behandlingTypeFilter,
  BehandlingTypeFilter,
  IOppgave,
  SoeknadTypeFilter,
  soeknadTypeFilter,
  statusFilter,
  StatusFilter,
} from './typer/oppgavebenken'
import { format } from 'date-fns'
import { Tag } from '@navikt/ds-react'
import HandlingerKnapp from './handlinger/HandlingerKnapp'
import BrukeroversiktLenke from './handlinger/BrukeroversiktKnapp'
import { tagColors } from '~shared/Tags'

export const kolonner: ReadonlyArray<Column<IOppgave>> = [
  {
    Header: 'Reg. dato',
    accessor: 'regdato',
    Cell: ({ value: dato }) => {
      return <span>{format(dato, 'dd.MM.yyyy')}</span>
    },
    sortType: 'datetime',
  },
  {
    Header: 'Frist',
    accessor: 'fristdato',
    Cell: ({ value: dato }) => {
      return <span>{format(dato, 'dd.MM.yyyy')}</span>
    },
    sortType: 'datetime',
  },
  {
    Header: 'Fødselsnummer',
    accessor: 'fnr',
    Cell: ({ value: fnr }) => {
      return <BrukeroversiktLenke fnr={fnr} />
    },
  },
  {
    Header: 'Behandlingstype',
    accessor: 'behandlingType',
    filter: 'exact',
    Cell: ({ value: oppgavetype }) => {
      return (
        <Tag variant={tagColors[oppgavetype]} size={'small'}>
          {behandlingTypeFilter[oppgavetype as BehandlingTypeFilter]?.navn}
        </Tag>
      )
    },
  },
  {
    Header: 'Ytelse',
    accessor: 'soeknadType',
    filter: 'exact',
    Cell: ({ value: soeknadstype }) => {
      return (
        <Tag variant={tagColors[soeknadstype]} size={'small'}>
          {soeknadTypeFilter[soeknadstype as SoeknadTypeFilter]?.navn}
        </Tag>
      )
    },
  },
  {
    Header: 'Søsken',
    accessor: 'antallSoesken',
    disableSortBy: true,
    Cell: ({ value: antallSoesken }) => {
      return <span>{Number.isInteger(antallSoesken) ? `${antallSoesken} søsken` : '-'}</span>
    },
  },
  {
    Header: 'Status',
    accessor: 'oppgaveStatus',
    filter: 'exact',
    Cell: ({ value: oppgaveStatus }) => {
      return (
        <span>{oppgaveStatus ? statusFilter[oppgaveStatus as StatusFilter]?.navn ?? oppgaveStatus : 'Ukjent'}</span>
      )
    },
  },
  {
    Header: 'Handlinger',
    accessor: 'handling',
    Cell: ({ row, value: handling }) => {
      return <HandlingerKnapp handling={handling} behandlingsId={row.original.behandlingId} person={row.original.fnr} />
    },
  },
]
