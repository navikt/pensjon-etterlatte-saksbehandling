import { VStack } from '@navikt/ds-react'
import React, { ReactNode } from 'react'
import { SortState } from '@navikt/ds-react'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { PagineringsKontroller } from '~components/oppgavebenk/oppgaver/PagineringsKontroller'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'
import { AlertIngenOppgaver } from '~components/oppgavebenk/utils/oppgaveFelles'

export interface Props {
  oppgaver: OppgaveDTO[]
  totaltAntall: number
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterSaksbehandlerTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
  oppdaterFrist: (id: string, nyfrist: string) => void
  oppdaterMerknad: (oppgaveId: string, merknad: string) => void
  page: number
  setPage: (page: number) => void
  rowsPerPage: number
  setRowsPerPage: (rows: number) => void
  sort: SortState | undefined
  handleSort: (sortKey: string) => void
  isLoading?: boolean
}

export const Oppgaver = ({
  oppgaver,
  totaltAntall,
  saksbehandlereIEnhet,
  oppdaterSaksbehandlerTildeling,
  oppdaterFrist,
  oppdaterStatus,
  oppdaterMerknad,
  page,
  setPage,
  rowsPerPage,
  setRowsPerPage,
  sort,
  handleSort,
  isLoading,
}: Props): ReactNode => {
  const antallSider = Math.ceil(totaltAntall / rowsPerPage)

  if (!isLoading && !oppgaver.length) return AlertIngenOppgaver

  return (
    <VStack gap="space-8">
      <PagineringsKontroller page={page} setPage={setPage} antallSider={antallSider} />
      <OppgaverTable
        oppgaver={oppgaver}
        oppdaterTildeling={oppdaterSaksbehandlerTildeling}
        oppdaterFrist={oppdaterFrist}
        oppdaterStatus={oppdaterStatus}
        oppdaterMerknad={oppdaterMerknad}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        sort={sort}
        handleSort={handleSort}
        isLoading={isLoading}
      />
      <PagineringsKontroller
        page={page}
        setPage={setPage}
        antallSider={antallSider}
        raderPerSide={rowsPerPage}
        setRaderPerSide={setRowsPerPage}
        totalAvOppgaverTeksts={`Viser ${Math.min((page - 1) * rowsPerPage + 1, totaltAntall)} - ${Math.min(page * rowsPerPage, totaltAntall)} av ${totaltAntall} oppgaver`}
      />
    </VStack>
  )
}
