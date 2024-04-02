import React, { ReactNode, useEffect, useState } from 'react'
import { SortState, Table } from '@navikt/ds-react'
import { OppgaverTableHeader } from '~components/oppgavebenk/oppgaverTable/OppgaverTableHeader'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { OppgaverTableRow } from '~components/oppgavebenk/oppgaverTable/OppgaverTableRow'
import {
  initialSortering,
  leggTilSorteringILocalStorage,
  OppgaveSortering,
} from '~components/oppgavebenk/utils/oppgaveSortering'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'

export enum SortKey {
  REGISTRERINGSDATO = 'registreringsdato',
  FRIST = 'frist',
  FNR = 'fnr',
}

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  oppdaterFrist?: (id: string, nyfrist: string, versjon: number | null) => void
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
  saksbehandlereIEnhet: Array<Saksbehandler>
  setSortering: (nySortering: OppgaveSortering) => void
}

export const OppgaverTable = ({
  oppgaver,
  oppdaterTildeling,
  oppdaterFrist,
  revurderingsaarsaker,
  saksbehandlereIEnhet,
  setSortering,
}: Props): ReactNode => {
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
      <OppgaverTableHeader />
      <Table.Body>
        {oppgaver?.map((oppgave: OppgaveDTO) => (
          <OppgaverTableRow
            key={`${oppgave.id}`}
            oppgave={oppgave}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            oppdaterTildeling={oppdaterTildeling}
            oppdaterFrist={oppdaterFrist}
            revurderingsaarsaker={revurderingsaarsaker}
          />
        ))}
      </Table.Body>
    </Table>
  )
}
