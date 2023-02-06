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
import { isSuccess, isPending, useApiCall } from '~shared/hooks/useApiCall'

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`

const Oppgavebenken = () => {
  const [oppgaver, fetchOppgaver] = useApiCall(hentOppgaver)
  const [oppgaveFelter, setOppgaveFelter] = useState<IOppgaveFelter>(initialOppgaveFelter())
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
    fetchOppgaver({})
  }, [])

  const columns: ReadonlyArray<Column<IOppgave>> = React.useMemo(() => kolonner, [])

  return (
    <OppgavebenkContainer>
      <>
        <OppgaveHeader
          oppgaveFelter={oppgaveFelter}
          setOppgaveFelter={setOppgaveFelter}
          setGlobalFilter={setGlobalFilter}
          henterOppgaver={() => fetchOppgaver({})}
        />
        <Spinner visible={isPending(oppgaver)} label={'Laster oppgaver'} />
        {isSuccess(oppgaver) && (
          <OppgaveListe
            columns={columns}
            data={oppgaver.data.oppgaver.map(mapOppgaveResponse)}
            globalFilterValue={globalFilter}
            filterPar={filterPar}
          />
        )}
      </>
    </OppgavebenkContainer>
  )
}

const mapOppgaveResponse = (data: OppgaveDTO): IOppgave => ({
  sakId: data.sakId,
  behandlingId: data.behandlingId,
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
})

export default Oppgavebenken
