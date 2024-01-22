import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import styled from 'styled-components'
import { Pagination } from '@navikt/ds-react'

interface Props {
  page: number
  setPage: Dispatch<SetStateAction<number>>
  antallSider: number
  raderPerSide: number
  setRaderPerSide: Dispatch<SetStateAction<number>>
  totalAvOppgaverTeksts: string
}

export const PagineringsKontroller = ({
  page,
  setPage,
  antallSider,
  raderPerSide,
  setRaderPerSide,
  totalAvOppgaverTeksts,
}: Props): ReactNode => {
  return (
    <PaginationWrapper>
      <Pagination page={page} onPageChange={setPage} count={antallSider} size="small" />
      <p>{totalAvOppgaverTeksts}</p>
      <select
        value={raderPerSide}
        onChange={(e) => {
          setRaderPerSide(Number(e.target.value))
        }}
        title="Antall oppgaver som vises"
      >
        {[10, 20, 30, 40, 50].map((rowsPerPage) => (
          <option key={rowsPerPage} value={rowsPerPage}>
            Vis {rowsPerPage} oppgaver
          </option>
        ))}
      </select>
    </PaginationWrapper>
  )
}

export const PaginationWrapper = styled.div`
  display: flex;
  gap: 0.5em;
  flex-wrap: wrap;
  margin: 0.5em 0;

  > p {
    margin: 0;
    line-height: 32px;
  }
`
