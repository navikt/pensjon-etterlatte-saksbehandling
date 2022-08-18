import { Column } from 'react-table'
import {
  IOppgave,
  behandlingTypeFilter,
  BehandlingTypeFilter,
  SoeknadTypeFilter,
  soeknadTypeFilter,
  statusFilter,
  StatusFilter,
} from './typer/oppgavebenken'
import { format } from 'date-fns'
import { ColorTag } from './styled'
import SaksbehandlerTildelKnapp from './filtere/SaksbehandlerTildelKnapp'
import HandlingerKnapp from './filtere/HandlingerKnapp'

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
  },
  {
    Header: 'Behandlingstype',
    accessor: 'behandlingType',
    filter: 'exact',
    Cell: ({ value: oppgavetype }) => {
      return <ColorTag type={oppgavetype} label={behandlingTypeFilter[oppgavetype as BehandlingTypeFilter]?.navn} />
    },
  },
  {
    Header: 'Ytelse',
    accessor: 'soeknadType',
    filter: 'exact',
    Cell: ({ value: soeknadstype }) => {
      return <ColorTag type={soeknadstype} label={soeknadTypeFilter[soeknadstype as SoeknadTypeFilter]?.navn} />
    },
  },
  {
    Header: 'Beskrivelse',
    accessor: 'beskrivelse',
    disableSortBy: true,
  },
  {
    Header: 'Søsken',
    accessor: 'antallSoesken',
    disableSortBy: true,
    Cell: ({ value: antallSoesken }) => {
      return <span>{antallSoesken ?? '-'}</span>
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
    Header: 'Saksbehandler',
    accessor: 'saksbehandler',
    filter: 'tildeltFilter',
    Cell: ({ value: saksbehandler }) => {
      return <SaksbehandlerTildelKnapp value={saksbehandler} />
    },
  },
  {
    Header: 'Handlinger',
    accessor: 'handling',
    Cell: ({ row, value: handling }) => {
      return <HandlingerKnapp handling={handling} behandlingsId={row.original.behandlingsId} />
    },
  },
]
