import React, { Dispatch, ReactNode, SetStateAction, useEffect, useState } from 'react'
import { SortState, Table } from '@navikt/ds-react'
import { OppgaverTableHeader } from '~components/oppgavebenk/oppgaverTable/OppgaverTableHeader'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { OppgaverTableRow } from '~components/oppgavebenk/oppgaverTable/OppgaverTableRow'
import { Filter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  alleOppgaver: Array<OppgaveDTO>
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  erMinOppgaveliste: boolean
  hentOppgaver: () => void
  filter: Filter
  setFilter: Dispatch<SetStateAction<Filter>>
}

export enum SortKey {
  FRIST = 'frist',
  FNR = 'fnr',
}

export const OppgaverTable = ({
  oppgaver,
  alleOppgaver,
  oppdaterTildeling,
  erMinOppgaveliste,
  hentOppgaver,
  filter,
  setFilter,
}: Props): ReactNode => {
  const [sort, setSort] = useState<SortState>()

  const handleSort = (sortKey: SortKey) => {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === 'descending'
        ? undefined
        : {
            orderBy: sortKey,
            direction: sort && sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'ascending',
          }
    )
  }

  useEffect(() => {
    switch (sort?.orderBy) {
      case SortKey.FRIST:
        setFilter({ ...filter, fristSortering: sort?.direction ? sort.direction : 'ingen' })
        break
      case SortKey.FNR:
        setFilter({ ...filter, fnrSortering: sort?.direction ? sort.direction : 'ingen' })
        break
    }
  }, [sort])

  return (
    <Table sort={sort} onSortChange={(sortKey) => handleSort(sortKey as SortKey)}>
      <OppgaverTableHeader />
      <Table.Body>
        {oppgaver &&
          oppgaver.map((oppgave: OppgaveDTO) => {
            return (
              <OppgaverTableRow
                key={oppgave.id}
                oppgave={oppgave}
                oppgaver={oppgaver}
                alleOppgaver={alleOppgaver}
                oppdaterTildeling={oppdaterTildeling}
                erMinOppgaveListe={erMinOppgaveliste}
                hentOppgaver={hentOppgaver}
              />
            )
          })}
      </Table.Body>
    </Table>
  )
}
