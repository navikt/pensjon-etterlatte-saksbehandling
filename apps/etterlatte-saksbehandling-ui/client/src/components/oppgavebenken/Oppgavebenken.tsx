import React, { useContext, useEffect, useState } from 'react'
import OppgaveHeader from './OppgaveHeader'
import OppgaveListe from './OppgaveListe'
import styled from 'styled-components'
import { FilterPar, IOppgave } from './typer/oppgavebenken'
import { Column } from 'react-table'
import { kolonner } from './OppgaveKolonner'
import { initialOppgaveFelter, IOppgaveFelter } from './typer/oppgavefelter'
import { hentMockOppgaver } from '../../shared/api/oppgaver'
import Spinner from '../../shared/Spinner'
import { AppContext, IAppContext } from '../../store/AppContext'

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`

const Oppgavebenken = () => {
  const saksbehandlerNavn = useContext<IAppContext>(AppContext).state.saksbehandlerReducer.navn
  const [lasterOppgaver, setLasterOppgaver] = useState(true)
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
    hentMockOppgaver()
      .then((oppgaver: ReadonlyArray<IOppgave>) => {
        setOppgaver(oppgaver)
        setLasterOppgaver(false)
      })
      .catch(() => {
        setLasterOppgaver(false)
        //todo: error håndtering her
      })
      .finally(() => setLasterOppgaver(false))
  }, [])

  const data: ReadonlyArray<IOppgave> = React.useMemo(() => oppgaver, [oppgaver])
  const columns: ReadonlyArray<Column<IOppgave>> = React.useMemo(() => kolonner, [])

  return (
    <OppgavebenkContainer>
      <Spinner visible={lasterOppgaver} label={'Laster saker'} />
      {!lasterOppgaver && (
        <>
          <OppgaveHeader
            oppgaveFelter={oppgaveFelter}
            setOppgaveFelter={setOppgaveFelter}
            setGlobalFilter={setGlobalFilter}
          />
          <OppgaveListe columns={columns} data={data} globalFilterValue={globalFilter} filterPar={filterPar} />
        </>
      )}
    </OppgavebenkContainer>
  )
}

export default Oppgavebenken
