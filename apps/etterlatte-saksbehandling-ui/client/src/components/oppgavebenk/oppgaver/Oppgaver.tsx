import { Alert } from '@navikt/ds-react'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import React, { ReactNode, useEffect, useState } from 'react'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { PagineringsKontroller } from '~components/oppgavebenk/oppgaver/PagineringsKontroller'
import {
  hentSorteringFraLocalStorage,
  OppgaveSortering,
  sorterFnr,
  sorterDato,
} from '~components/oppgavebenk/utils/oppgaveSortering'
import { Saksbehandler } from '~shared/types/saksbehandler'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  hentPagineringSizeFraLocalStorage,
  leggTilOppgavenIMinliste,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { filtrerOppgaver } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useAppSelector } from '~store/Store'

export interface OppgavelisteProps {
  oppgaver: OppgaveDTO[]
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterFrist?: (id: string, nyfrist: string, versjon: number | null) => void
  filter?: Filter
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
}

export const Oppgaver = ({
  oppgaver,
  saksbehandlereIEnhet,
  oppdaterFrist,
  filter,
  revurderingsaarsaker,
}: OppgavelisteProps): ReactNode => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [sortering, setSortering] = useState<OppgaveSortering>(hentSorteringFraLocalStorage())
  const filtrerteOppgaver = filter
    ? filtrerOppgaver(
        filter.sakEllerFnrFilter,
        filter.enhetsFilter,
        filter.fristFilter,
        filter.saksbehandlerFilter,
        filter.ytelseFilter,
        filter.oppgavestatusFilter,
        filter.oppgavetypeFilter,
        [...oppgaver]
      )
    : oppgaver

  const sortertRegistreringsdato = sorterDato(sortering.registreringsdatoSortering, filtrerteOppgaver)
  const sortertFrist = sorterDato(sortering.fristSortering, sortertRegistreringsdato)
  const sorterteOppgaver = sorterFnr(sortering.fnrSortering, sortertFrist)

  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(hentPagineringSizeFraLocalStorage())

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  let paginerteOppgaver = sorterteOppgaver

  useEffect(() => {
    if (paginerteOppgaver.length === 0 && filtrerteOppgaver.length > 0) setPage(1)
  }, [sorterteOppgaver, filtrerteOppgaver])

  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  if (!paginerteOppgaver.length) return <Alert variant="info">Ingen oppgaver</Alert>

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterSaksbehandlerTildeling(oppgavebenkState.oppgavelistaOppgaver, oppgave.id, saksbehandler, versjon)
      )
      if (innloggetSaksbehandler.ident === saksbehandler?.ident) {
        dispatcher.setMinOppgavelisteOppgaver(
          sorterOppgaverEtterOpprettet(
            leggTilOppgavenIMinliste(oppgavebenkState.minOppgavelisteOppgaver, oppgave, saksbehandler, versjon)
          )
        )
      } else {
        dispatcher.setMinOppgavelisteOppgaver(
          sorterOppgaverEtterOpprettet(
            filtrerKunInnloggetBrukerOppgaver(
              finnOgOppdaterSaksbehandlerTildeling(
                oppgavebenkState.minOppgavelisteOppgaver,
                oppgave.id,
                saksbehandler,
                versjon
              )
            )
          )
        )
      }
    }, 2000)
  }

  return (
    <>
      <PagineringsKontroller
        page={page}
        setPage={setPage}
        antallSider={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
      />

      <OppgaverTable
        oppgaver={paginerteOppgaver}
        oppdaterTildeling={oppdaterSaksbehandlerTildeling}
        oppdaterFrist={oppdaterFrist}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        setSortering={setSortering}
        revurderingsaarsaker={revurderingsaarsaker}
      />

      <PagineringsKontroller
        page={page}
        setPage={setPage}
        antallSider={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
        raderPerSide={rowsPerPage}
        setRaderPerSide={setRowsPerPage}
        totalAvOppgaverTeksts={`Viser ${(page - 1) * rowsPerPage + 1} - ${
          (page - 1) * rowsPerPage + paginerteOppgaver.length
        } av ${filtrerteOppgaver.length} oppgaver ${oppgaver.length ? `(totalt ${oppgaver.length} oppgaver)` : ''}`}
      />
    </>
  )
}
