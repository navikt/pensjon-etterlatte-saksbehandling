import React, { ReactNode, useEffect, useState } from 'react'
import { SortState, Table } from '@navikt/ds-react'
import {
  initialSortering,
  leggTilSorteringILocalStorage,
  OppgaveSortering,
} from '~components/oppgavebenk/utils/oppgaveSortering'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { GosysOppgave } from '~shared/types/Gosys'
import { GosysOppgaveRow } from '~components/oppgavebenk/gosys/GosysOppgaveRow'

export enum SortKey {
  REGISTRERINGSDATO = 'registreringsdato',
  FRIST = 'frist',
  FNR = 'fnr',
}

interface Props {
  oppgaver: ReadonlyArray<GosysOppgave>
  saksbehandlereIEnhet: Array<Saksbehandler>
  setSortering: (nySortering: OppgaveSortering) => void
}

export const GosysOppgaverTable = ({ oppgaver, saksbehandlereIEnhet, setSortering }: Props): ReactNode => {
  const [sort, setSort] = useState<SortState>()

  const handleSort = (sortKey: SortKey) => {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === 'descending'
        ? { orderBy: sortKey, direction: 'none' }
        : {
            orderBy: sortKey,
            direction: sort && sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'ascending',
          }
    )
  }

  useEffect(() => {
    switch (sort?.orderBy) {
      case SortKey.REGISTRERINGSDATO:
        const nySorteringRegistreringsdato: OppgaveSortering = {
          ...initialSortering,
          registreringsdatoSortering: sort ? sort.direction : 'none',
        }
        setSortering(nySorteringRegistreringsdato)
        leggTilSorteringILocalStorage(nySorteringRegistreringsdato)
        break
      case SortKey.FRIST:
        const nySorteringFrist: OppgaveSortering = {
          ...initialSortering,
          fristSortering: sort ? sort.direction : 'none',
        }
        setSortering(nySorteringFrist)
        leggTilSorteringILocalStorage(nySorteringFrist)
        break
      case SortKey.FNR:
        const nySorteringFnr: OppgaveSortering = {
          ...initialSortering,
          fnrSortering: sort ? sort.direction : 'none',
        }
        setSortering(nySorteringFnr)
        leggTilSorteringILocalStorage(nySorteringFnr)
        break
    }
  }, [sort])

  return (
    <Table
      size="small"
      sort={sort && sort.direction !== 'none' ? { direction: sort.direction, orderBy: sort.orderBy } : undefined}
      onSortChange={(sortKey) => handleSort(sortKey as SortKey)}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col" sortKey={SortKey.REGISTRERINGSDATO} sortable>
            Opprettet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey={SortKey.FRIST} sortable>
            Frist
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey={SortKey.FNR} sortable>
            Fødselsnummer
          </Table.ColumnHeader>
          <Table.HeaderCell scope="col">Tema</Table.HeaderCell>
          <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
          <Table.HeaderCell scope="col">Beskrivelse</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
          <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
          <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
          <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {oppgaver?.map((oppgave: GosysOppgave) => (
          <GosysOppgaveRow key={oppgave.id} oppgave={oppgave} saksbehandlereIEnhet={saksbehandlereIEnhet} />
        ))}
      </Table.Body>
    </Table>
  )
}
