import React, { useEffect, useState } from 'react'
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
import { hentOppgaver, OppgaveDTO } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { useAppSelector } from '~store/Store'

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`

const Oppgavebenken = () => {
  const saksbehandlerNavn = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler.navn)
  const [lasterOppgaver, setLasterOppgaver] = useState(true)
  const [toggleHentOppgaver, setToggleHentOppgaver] = useState(false)
  const [oppgaver, setOppgaver] = useState<ReadonlyArray<IOppgave>>([])
  const [oppgaveFelter, setOppgaveFelter] = useState<IOppgaveFelter>(initialOppgaveFelter(saksbehandlerNavn))
  const [globalFilter, setGlobalFilter] = useState<string | undefined>('')
  const [filterPar, setFilterPar] = useState<Array<FilterPar>>([])
  const [val, setVal] = useState<boolean>(false)

  useEffect(() => {
    const filterPar = hentFilterFraOppgaveObject(oppgaveFelter)
    setFilterPar(filterPar)
  }, [oppgaveFelter])

  const createError = () => {
    const scriptError = document.querySelector('#script-error')
    scriptError!!.addEventListener('click', () => {
      const badCode = 'const s;'
      eval(badCode)
    })
  }

  const hentFilterFraOppgaveObject = (oppgaveFelter: IOppgaveFelter): Array<FilterPar> => {
    const setValue = (value: string | undefined) => {
      return value === 'VELG' || value === '' || value === 'ALLE' ? undefined : value
    }
    return Object.values(oppgaveFelter)
      .filter((felt) => felt.filter)
      .map((felt) => ({ id: felt.noekkel, value: setValue(felt.filter?.selectedValue) }))
  }

  useEffect(() => {
    const lastOppgaver = async () => {
      const response = await hentOppgaver()

      if (response.status === 'ok') {
        setOppgaver(response.data.oppgaver.map(mapOppgaveResponse))
      }

      setLasterOppgaver(false)
    }

    setLasterOppgaver(true)
    lastOppgaver()
  }, [toggleHentOppgaver])

  const data: ReadonlyArray<IOppgave> = React.useMemo(() => oppgaver, [oppgaver])
  const columns: ReadonlyArray<Column<IOppgave>> = React.useMemo(() => kolonner, [])
  if (val) {
    throw new Error('I crashed!')
  }
  return (
    <OppgavebenkContainer>
      <>
        <button onClick={() => setVal(true)}>Klikk her for å generere feil med errorcomponent</button>
        <button onClick={createError}>Test; klikk her for å få teste feil-logging</button>
        <OppgaveHeader
          oppgaveFelter={oppgaveFelter}
          setOppgaveFelter={setOppgaveFelter}
          setGlobalFilter={setGlobalFilter}
          henterOppgaver={() => setToggleHentOppgaver(!toggleHentOppgaver)}
        />
        <Spinner visible={lasterOppgaver} label={'Laster oppgaver'} />
        {!lasterOppgaver && (
          <OppgaveListe columns={columns} data={data} globalFilterValue={globalFilter} filterPar={filterPar} />
        )}
      </>
    </OppgavebenkContainer>
  )
}

function mapOppgaveResponse(data: OppgaveDTO): IOppgave {
  const oppgave: IOppgave = {
    sakId: data.sakId,
    behandlingsId: data.behandlingsId,
    regdato: new Date(data.regdato),
    soeknadType: data.soeknadType.toUpperCase() as SoeknadTypeFilter,
    behandlingType: data.behandlingType.toUpperCase() as BehandlingTypeFilter,
    fristdato: new Date(data.fristdato),
    fnr: data.fnr,
    beskrivelse: data.beskrivelse,
    oppgaveStatus: data.oppgaveStatus.toUpperCase() as StatusFilter,
    saksbehandler: data.saksbehandler,
    handling: data.handling.toUpperCase() as Handlinger,
    antallSoesken: data.antallSoesken,
  }
  return oppgave
}

export default Oppgavebenken
