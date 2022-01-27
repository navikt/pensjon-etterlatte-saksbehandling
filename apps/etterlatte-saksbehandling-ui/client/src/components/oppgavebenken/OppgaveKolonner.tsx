import { Column } from 'react-table'
import {
  Handlinger,
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
    Header: 'Oppgavetype',
    accessor: 'behandlingType',
    filter: 'exact',
    Cell: ({ value: oppgavetype }) => {
      return <ColorTag type={oppgavetype} label={behandlingTypeFilter[oppgavetype as BehandlingTypeFilter]?.navn} />
    },
  },
  {
    Header: 'Søknadstype',
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

export const mockdata: ReadonlyArray<IOppgave> = [
  {
    regdato: new Date(2022, 1, 6),
    soeknadType: SoeknadTypeFilter.BARNEPENSJON,
    behandlingType: BehandlingTypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    fnr: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Truls Veileder',
    handling: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 12),
    behandlingType: BehandlingTypeFilter.FOERSTEGANGSBEHANDLING,
    soeknadType: SoeknadTypeFilter.GJENLEVENDEPENSJON,
    fristdato: new Date(2022, 1, 6),
    fnr: '123451111',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Test Testulfsen',
    handling: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 23),
    soeknadType: SoeknadTypeFilter.BARNEPENSJON,
    behandlingType: BehandlingTypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    fnr: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    saksbehandler: 'Truls Veileder',
    handling: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 22),
    soeknadType: SoeknadTypeFilter.GJENLEVENDEPENSJON,
    behandlingType: BehandlingTypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    fnr: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    saksbehandler: '',
    handling: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 12),
    soeknadType: SoeknadTypeFilter.BARNEPENSJON,
    behandlingType: BehandlingTypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    fnr: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    saksbehandler: '',
    handling: Handlinger.START,
  },
  {
    regdato: new Date(2022, 3, 16),
    soeknadType: SoeknadTypeFilter.BARNEPENSJON,
    behandlingType: BehandlingTypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    fnr: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Truls Veileder',
    handling: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 15),
    soeknadType: SoeknadTypeFilter.BARNEPENSJON,
    behandlingType: BehandlingTypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    fnr: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Truls Veileder2',
    handling: Handlinger.START,
  },
]
