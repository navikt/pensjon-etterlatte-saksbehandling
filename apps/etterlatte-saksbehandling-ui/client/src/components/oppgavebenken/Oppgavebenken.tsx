import React, { useContext, useEffect, useState } from 'react'
import OppgaveHeader from './OppgaveHeader'
import OppgaveListe from './OppgaveListe'
import styled from 'styled-components'
import {
  BehandlingTypeFilter,
  FilterPar,
  Handlinger,
  IOppgave,
  SoeknadTypeFilter,
  StatusFilter,
} from './typer/oppgavebenken'
import { Column } from 'react-table'
import { kolonner } from './OppgaveKolonner'
import { initialOppgaveFelter, IOppgaveFelter } from './typer/oppgavefelter'
import { hentOppgaver } from '../../shared/api/oppgaver'
import Spinner from '../../shared/Spinner'
import { AppContext, IAppContext } from '../../store/AppContext'
import { IApiResponse } from '../../shared/api/types'
import moment from 'moment'

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`

const Oppgavebenken = () => {
  const saksbehandlerNavn = useContext<IAppContext>(AppContext).state.saksbehandlerReducer.navn
  const [lasterOppgaver, setLasterOppgaver] = useState(true)
  const [hentOppgaverKlikket, setHentOppgaverKlikket] = useState(false)
  const [oppgaver, setOppgaver] = useState<ReadonlyArray<IOppgave>>([])
  const [oppgaveFelter, setOppgaveFelter] = useState<IOppgaveFelter>(initialOppgaveFelter(saksbehandlerNavn))
  const [globalFilter, setGlobalFilter] = useState<string | undefined>('')
  const [filterPar, setFilterPar] = useState<Array<FilterPar>>([])

  useEffect(() => {
    const filterPar = hentFilterFraOppgaveObject(oppgaveFelter)
    setFilterPar(filterPar)
  }, [oppgaveFelter])

  const hentFilterFraOppgaveObject = (oppgaveFelter: IOppgaveFelter): Array<FilterPar> => {
    const setValue = (value: string | undefined) => {
      return value === 'VELG' || value === '' || value === 'ALLE' ? undefined : value
    }
    return Object.values(oppgaveFelter)
      .filter((felt) => felt.filter)
      .map((felt) => ({ id: felt.noekkel, value: setValue(felt.filter?.selectedValue) }))
  }

  useEffect(() => {
    hentOppgaver()
      .then((response: IApiResponse<any>) => {
        const mappedResponse = response.data.map((oppgave: any) => mapOppgaveResponse(oppgave))
        setOppgaver(mappedResponse)
        setLasterOppgaver(false)
      })
      .catch(() => {
        setLasterOppgaver(false)
      })
      .finally(() => {
        setLasterOppgaver(false)
        setHentOppgaverKlikket(false)
      })
  }, [hentOppgaverKlikket])

  const data: ReadonlyArray<IOppgave> = React.useMemo(() => oppgaver, [oppgaver])
  const columns: ReadonlyArray<Column<IOppgave>> = React.useMemo(() => kolonner, [])

  return (
    <OppgavebenkContainer>
      <>
        <OppgaveHeader
          oppgaveFelter={oppgaveFelter}
          setOppgaveFelter={setOppgaveFelter}
          setGlobalFilter={setGlobalFilter}
          henterOppgaver={() => setHentOppgaverKlikket(true)}
        />
        <Spinner visible={lasterOppgaver} label={'Laster saker'} />
        {!lasterOppgaver && (
          <OppgaveListe columns={columns} data={data} globalFilterValue={globalFilter} filterPar={filterPar} />
        )}
      </>
    </OppgavebenkContainer>
  )
}

function mapOppgaveResponse(data: any): IOppgave {
  const oppgave: IOppgave = {
    sakId: data.sakId,
    behandlingsId: data.sakId,
    regdato: moment(data.regdato, 'YYYY-MM-DD').toDate(),
    soeknadType: data.soeknadType.toUpperCase() as SoeknadTypeFilter,
    behandlingType: data.behandlingType.toUpperCase() as BehandlingTypeFilter,
    fristdato: moment(data.fristdato, 'YYYY-MM-DD').toDate(),
    fnr: data.fnr,
    beskrivelse: data.beskrivelse,
    status: mapStatus(data.status.toUpperCase()),
    saksbehandler: data.saksbehandler,
    handling: data.handling.toUpperCase() as Handlinger,
  }
  return oppgave
}

function mapStatus(status: string): StatusFilter {
  if (status === 'OPPRETTET') {
    return StatusFilter.NY
  } else {
    return StatusFilter.UNDER_BEHANDLING
  }
}

export default Oppgavebenken
