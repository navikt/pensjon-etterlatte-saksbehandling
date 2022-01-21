import { Column } from 'react-table'
import {
  Handlinger,
  IOppgave,
  oppgavetypeFilter,
  OppgavetypeFilter,
  SoeknadstypeFilter,
  soeknadstypeFilter,
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
    accessor: 'oppgavetype',
    filter: 'exact',
    Cell: ({ value: oppgavetype }) => {
      return <ColorTag type={oppgavetype} label={oppgavetypeFilter[oppgavetype as OppgavetypeFilter]?.navn} />
    },
  },
  {
    Header: 'Søknadstype',
    accessor: 'soeknadstype',
    filter: 'exact',
    Cell: ({ value: soeknadstype }) => {
      return <ColorTag type={soeknadstype} label={soeknadstypeFilter[soeknadstype as SoeknadstypeFilter]?.navn} />
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
    Header: 'Saksbehandler',
    accessor: 'saksbehandler',
    filter: 'tildeltFilter',
    Cell: ({ value: saksbehandler }) => {
      return <SaksbehandlerFilterListe value={saksbehandler} />
    },
  },
  {
    Header: 'Handlinger',
    accessor: 'handlinger',
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
    soeknadstype: SoeknadstypeFilter.BARNEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Truls Veileder',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 12),
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    fristdato: new Date(2022, 1, 6),
    bruker: '123451111',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Test Testulfsen',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 23),
    soeknadstype: SoeknadstypeFilter.BARNEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    saksbehandler: 'Truls Veileder',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 22),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    saksbehandler: '',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 12),
    soeknadstype: SoeknadstypeFilter.BARNEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    saksbehandler: '',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2022, 3, 16),
    soeknadstype: SoeknadstypeFilter.BARNEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Truls Veileder',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 15),
    soeknadstype: SoeknadstypeFilter.BARNEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Truls Veileder2',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 14),
    soeknadstype: SoeknadstypeFilter.BARNEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    saksbehandler: '',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 13),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    saksbehandler: 'Truls Veileder',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 3, 12),
    fristdato: new Date(2022, 1, 6),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    saksbehandler: 'Truls Veileder2',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 6, 12),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 1',
    status: StatusFilter.FERDIG,
    saksbehandler: 'Truls Veileder',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 5, 12),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 2',
    status: StatusFilter.FERDIG,
    saksbehandler: '',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2020, 4, 12),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 3',
    status: StatusFilter.NY,
    saksbehandler: '',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2021, 3, 12),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 4',
    status: StatusFilter.NY,
    saksbehandler: '',
    handlinger: Handlinger.START,
  },
  {
    regdato: new Date(2022, 3, 12),
    soeknadstype: SoeknadstypeFilter.GJENLEVENDEPENSJON,
    oppgavetype: OppgavetypeFilter.FOERSTEGANGSBEHANDLING,
    fristdato: new Date(2022, 1, 6),
    bruker: '123456789',
    beskrivelse: 'test 5',
    status: StatusFilter.UNDER_BEHANDLING,
    saksbehandler: '',
    handlinger: Handlinger.START,
  },
]
