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
import moment from 'moment'
import { ColorTag } from './styled'
import React from 'react'
import SaksbehandlerFilterListe from './filtere/SaksbehandlerFilterListe'
import { Button } from '@navikt/ds-react'

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
    Header: 'Frist',
    accessor: 'fristdato',
    Cell: ({ value: dato }) => {
      return moment(dato).format()
    },
    sortType: 'datetime',
  },
  {
    Header: 'Fødselsnummer',
    accessor: 'fnr',
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
    Header: 'Saksbehandler',
    accessor: 'saksbehandler',
    filter: 'tildeltFilter',
    Cell: ({ value: saksbehandler }) => {
      return <SaksbehandlerFilterListe value={saksbehandler} />
    },
  },
  {
    Header: 'Handlinger',
    accessor: 'handling',
    Cell: ({ value: handling }) => {
      return (
        <Button size={'small'} onClick={() => {}} variant={'secondary'}>
          {handling.toString()}
        </Button>
      )
    },
  },
]
