import React, { Dispatch, ReactNode, SetStateAction, useEffect, useState } from 'react'
import { SortState, Table } from '@navikt/ds-react'
import { OppgaverTableHeader } from '~components/oppgavebenk/oppgaverTable/OppgaverTableHeader'
import { OppgaveDTO, Saksbehandler } from '~shared/api/oppgaver'
import { OppgaverTableRow } from '~components/oppgavebenk/oppgaverTable/OppgaverTableRow'
import { Filter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

export enum SortKey {
  FRIST = 'frist',
  FNR = 'fnr',
}

interface SorteringsState extends Omit<SortState, 'direction'> {
  direction: 'ascending' | 'descending' | 'no-order'
}

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  erMinOppgaveliste: boolean
  oppdaterFrist: (id: string, nyfrist: string, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  filter: Filter
  setFilter: Dispatch<SetStateAction<Filter>>
}

export const OppgaverTable = ({
  oppgaver,
  oppdaterTildeling,
  erMinOppgaveliste,
  oppdaterFrist,
  saksbehandlereIEnhet,
  filter,
  setFilter,
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
  }

  useEffect(() => {
    switch (sort?.orderBy) {
      case SortKey.FRIST:
        setFilter({ ...filter, fristSortering: sort ? sort.direction : 'no-order' })
        break
      case SortKey.FNR:
        setFilter({ ...filter, fnrSortering: sort ? sort.direction : 'no-order' })
        break
    }
  }, [sort])

  return (
    <Table
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
                erMinOppgaveListe={erMinOppgaveliste}
                oppdaterFrist={oppdaterFrist}
              />
            )
          })}
      </Table.Body>
    </Table>
  )
}
