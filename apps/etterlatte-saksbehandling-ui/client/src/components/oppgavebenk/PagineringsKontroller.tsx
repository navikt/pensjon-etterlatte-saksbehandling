import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import { BodyShort, Pagination } from '@navikt/ds-react'
import { leggTilPagineringLocalStorage, pagineringslisteverdier } from '~components/oppgavebenk/oppgaveutils'
import { FlexRow } from '~shared/styled'

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
    <div style={{ marginTop: '1rem' }}>
      <FlexRow justify="center" $spacing>
        <Pagination page={page} onPageChange={setPage} count={antallSider} size="small" />
      </FlexRow>

      <FlexRow justify="center" align="center" $spacing>
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
      </FlexRow>
    </div>
  )
}
