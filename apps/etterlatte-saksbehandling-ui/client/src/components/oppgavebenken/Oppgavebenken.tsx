import React, { useEffect, useState } from 'react'
import OppgaveHeader from './OppgaveHeader'
import OppgaveListe from './OppgaveListe'

import styled from 'styled-components'
import { IOppgave } from '../../typer/oppgavebenken'
import { Column } from 'react-table'
import { kolonner } from './OppgaveUtils'
import { initialOppgaveFelter, IOppgaveFelter } from './oppgavefelter'
import { hentOppgaver } from '../../shared/api/oppgaver'
import Spinner from '../../shared/Spinner'

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`

const Oppgavebenken = () => {
  const [lasterOppgaver, settLasterOppgaver] = useState(true)
  const [oppgaver, settOppgaver] = useState<ReadonlyArray<IOppgave>>([])

  const [oppgaveFelter, settOppgaveFelter] = useState<IOppgaveFelter>(initialOppgaveFelter())

  //const [oppgaveFilter, settOppgaveFilter] = useState<IOppgaveFelter>()

  const [globalFilter, settGlobalFilter] = useState<string>('')

  useEffect(() => {
    hentOppgaver()
      .then((oppgaver: ReadonlyArray<IOppgave>) => {
        settOppgaver(oppgaver)
        settLasterOppgaver(false)
      })
      .catch(() => {
        settLasterOppgaver(false)
        //todo: error håndtering her
      })
      .finally(() => settLasterOppgaver(false))
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
            settOppgaveFelter={settOppgaveFelter}
            settGlobalFilter={settGlobalFilter}
          />
          <OppgaveListe columns={columns} data={data} globalFilter={globalFilter} />
        </>
      )}
    </OppgavebenkContainer>
  )
}

export default Oppgavebenken
