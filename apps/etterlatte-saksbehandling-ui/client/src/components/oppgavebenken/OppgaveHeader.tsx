import React, { useState } from 'react'
import { Button, Heading } from '@navikt/ds-react'
import { initialOppgaveFelter, IOppgaveFelter } from './oppgavefelter'
import '../../App.css'
import styled from 'styled-components'
import 'react-datepicker/dist/react-datepicker.css'
import '../../index.css'
import moment from 'moment'
import { GlobalFilter } from './filtere/GlobalFilter'
import ColumnFilters from './filtere/ColumnFilters'

moment.defaultFormat = 'DD.MM.YYYY'

type Props = {
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
  setGlobalFilter: (value: string | undefined) => void
}

const OppgaveHeader: React.FC<Props> = ({ oppgaveFelter, setOppgaveFelter, setGlobalFilter }) => {
  const [resetGlobalInput, setResetGlobalInput] = useState<boolean>(false)

  return (
    <>
      <Heading size={'medium'} spacing>
        Oppgavebenken
      </Heading>
      <GlobalFilter
        setGlobalFilter={setGlobalFilter}
        resetGlobalInput={resetGlobalInput}
        setResetGlobalInput={setResetGlobalInput}
      />
      <ColumnFilters oppgaveFelter={oppgaveFelter} setOppgaveFelter={setOppgaveFelter} />
      <ButtonWrapper>
        <Button size={'small'} onClick={() => {}} variant={'primary'}>
          Hent
        </Button>
        <Button
          size={'small'}
          onClick={() => {
            setOppgaveFelter(initialOppgaveFelter())
            setResetGlobalInput(true)
          }}
          variant={'secondary'}
        >
          Tilbakestill alle filtre
        </Button>
      </ButtonWrapper>
    </>
  )
}

const ButtonWrapper = styled.div`
  margin-bottom: 3rem;
  display: flex;
  flex-direction: row;

  button {
    margin-right: 1rem;
  }
`

export default OppgaveHeader
