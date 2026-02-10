import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import { BodyShort, HStack, Pagination } from '@navikt/ds-react'
import { leggTilPagineringLocalStorage, pagineringslisteverdier } from '~components/oppgavebenk/utils/oppgaveHandlinger'

interface Props {
  page: number
  setPage: Dispatch<SetStateAction<number>>
  antallSider: number
  raderPerSide?: number
  setRaderPerSide?: Dispatch<SetStateAction<number>>
  totalAvOppgaverTeksts?: string
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
    <HStack gap="space-4" justify="center" align="center">
      <Pagination page={page} onPageChange={setPage} count={antallSider} size="small" />

      {totalAvOppgaverTeksts && <BodyShort>{totalAvOppgaverTeksts}</BodyShort>}

      {raderPerSide && setRaderPerSide && (
        <select
          value={raderPerSide}
          onChange={(e) => {
            const size = Number(e.target.value)
            setRaderPerSide(size)
            leggTilPagineringLocalStorage(size)
          }}
          title="Antall oppgaver som vises"
        >
          {pagineringslisteverdier.map((rowsPerPage) => (
            <option key={rowsPerPage} value={rowsPerPage}>
              Vis {rowsPerPage} oppgaver
            </option>
          ))}
        </select>
      )}
    </HStack>
  )
}
