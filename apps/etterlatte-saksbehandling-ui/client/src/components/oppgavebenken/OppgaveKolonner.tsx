import { Column } from 'react-table'
import {
  IOppgave,
  oppgaveTypeFilter,
  OppgaveTypeFilter,
  SoeknadTypeFilter,
  soeknadTypeFilter,
  statusFilter,
  StatusFilter,
  enhetFilter,
  EnhetFilter,
} from './typer/oppgavebenken'
import { format } from 'date-fns'
import { Tag } from '@navikt/ds-react'
import HandlingerKnapp from './handlinger/HandlingerKnapp'
import SaksoversiktLenke from './handlinger/BrukeroversiktKnapp'
import { tagColors } from '~shared/Tags'

const mapEnhetNavn = (enhet?: EnhetFilter): string => {
  const navn = (enhet ? enhetFilter[enhet as EnhetFilter]?.navn : undefined) ?? 'Ukjent'

  return navn.includes(' - ') ? navn.substring(0, navn.indexOf(' - ')) : navn
}

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
      return <SaksoversiktLenke fnr={fnr} />
    },
  },
  {
    Header: 'Behandlingstype',
    accessor: 'oppgaveType',
    filter: 'exact',
    Cell: ({ value: oppgavetype }) => {
      return (
        <Tag variant={tagColors[oppgavetype]} size={'small'}>
          {oppgaveTypeFilter[oppgavetype as OppgaveTypeFilter]?.navn}
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
    Header: 'Merknad',
    accessor: 'merknad',
    disableSortBy: true,
    Cell: ({ value: merknad }) => {
      return <span>{merknad || ''}</span>
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
    Header: 'Enhet',
    accessor: 'oppgaveEnhet',
    filter: 'exact',
    Cell: ({ value: enhet }) => {
      return <span>{mapEnhetNavn(enhet)}</span>
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
