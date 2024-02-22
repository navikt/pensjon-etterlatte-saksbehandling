import React, { ReactNode, useState } from 'react'
import { SortState, Table } from '@navikt/ds-react'
import { OppgaverTableHeader } from '~components/oppgavebenk/oppgaverTable/OppgaverTableHeader'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { OppgaverTableRow } from '~components/oppgavebenk/oppgaverTable/OppgaverTableRow'
import { leggTilSorteringILocalStorage, OppgaveSortering } from '~components/oppgavebenk/oppgaverTable/oppgavesortering'
import { Saksbehandler } from '~shared/types/saksbehandler'

export enum SortKey {
  FRIST = 'frist',
  FNR = 'fnr',
}

interface SorteringsState extends Omit<SortState, 'direction'> {
  direction: 'ascending' | 'descending' | 'no-order'
}

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  oppdaterFrist?: (id: string, nyfrist: string, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  setSortering: (nySortering: OppgaveSortering) => void
}

export const OppgaverTable = ({
  oppgaver,
  oppdaterTildeling,
  oppdaterFrist,
  saksbehandlereIEnhet,
  setSortering,
}: Props): ReactNode => {
  const [sort, setSort] = useState<SorteringsState>()

  const handleSort = (sortKey: SortKey) => {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === 'descending'
        ? { orderBy: sortKey, direction: 'no-order' }
        : {
            orderBy: sortKey,
            direction: sort && sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'ascending',
          }
    )
    switch (sort?.orderBy) {
      case SortKey.FRIST:
        const nySorteringFrist: OppgaveSortering = {
          fnrSortering: 'no-order',
          fristSortering: sort ? sort.direction : 'no-order',
        }
        setSortering(nySorteringFrist)
        leggTilSorteringILocalStorage(nySorteringFrist)
        break
      case SortKey.FNR:
        const nySorteringFnr: OppgaveSortering = {
          fristSortering: 'no-order',
          fnrSortering: sort ? sort.direction : 'no-order',
        }
        setSortering(nySorteringFnr)
        leggTilSorteringILocalStorage(nySorteringFnr)
        break
    }
  }

  return (
    <Table
      size="small"
      sort={sort && sort.direction !== 'no-order' ? { direction: sort.direction, orderBy: sort.orderBy } : undefined}
      onSortChange={(sortKey) => handleSort(sortKey as SortKey)}
    >
      <OppgaverTableHeader />
      <Table.Body>
        {oppgaver &&
          oppgaver.map((oppgave: OppgaveDTO) => {
            return (
              <OppgaverTableRow
                key={oppgave.id}
                oppgave={oppgave}
                saksbehandlereIEnhet={saksbehandlereIEnhet}
                oppdaterTildeling={oppdaterTildeling}
                oppdaterFrist={oppdaterFrist}
              />
            )
          })}
      </Table.Body>
    </Table>
  )
}
