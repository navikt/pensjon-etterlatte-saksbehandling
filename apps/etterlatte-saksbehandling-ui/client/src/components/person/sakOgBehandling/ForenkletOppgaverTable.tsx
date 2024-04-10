import React, { ReactNode, useEffect, useState } from 'react'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import { OppgavetypeTag, SaktypeTag } from '~components/oppgavebenk/components/Tags'
import { OPPGAVESTATUSFILTER } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { RevurderingsaarsakerDefault } from '~shared/types/Revurderingaarsak'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { HandlingerForOppgave } from '~components/oppgavebenk/components/HandlingerForOppgave'
import { useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { OppgaveValg } from '~components/person/sakOgBehandling/SakOversikt'

const aktiveOppgavestatuser = [Oppgavestatus.NY, Oppgavestatus.UNDER_BEHANDLING]

export const ForenkletOppgaverTable = ({
  oppgaver,
  oppgaveValg,
}: {
  oppgaver: OppgaveDTO[]
  oppgaveValg: OppgaveValg
}): ReactNode => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const filtrerOppgaverPaaOppgaveValg = (): OppgaveDTO[] => {
    switch (oppgaveValg) {
      case OppgaveValg.AKTIVE:
        return [...oppgaver].filter((oppgave) => aktiveOppgavestatuser.some((el) => oppgave.status.includes(el)))
      case OppgaveValg.FERDIGSTILTE:
        return [...oppgaver].filter((oppgave) => !aktiveOppgavestatuser.some((el) => oppgave.status.includes(el)))
    }
  }

  const [filtrerteOpgpaver, setFiltrerteOppgaver] = useState<OppgaveDTO[]>(filtrerOppgaverPaaOppgaveValg())

  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  const [, saksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)

  useEffect(() => {
    setFiltrerteOppgaver(filtrerOppgaverPaaOppgaveValg())
  }, [oppgaveValg])

  useEffect(() => {
    if (!!innloggetSaksbehandler.enheter.length) {
      saksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, setSaksbehandlereIEnheter)
    }
  }, [])

  return (
    <Table zebraStripes size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Reg.dato</Table.HeaderCell>
          <Table.HeaderCell scope="col">Frist</Table.HeaderCell>
          <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
          <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
          <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
          <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
          <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {!!filtrerteOpgpaver?.length &&
          filtrerteOpgpaver.map((oppgave: OppgaveDTO) => (
            <Table.Row key={oppgave.id}>
              <Table.DataCell>{formaterStringDato(oppgave.opprettet)}</Table.DataCell>
              <Table.DataCell>
                <FristWrapper dato={oppgave.frist} />
              </Table.DataCell>
              <Table.DataCell>
                <SaktypeTag sakType={oppgave.sakType} />
              </Table.DataCell>
              <Table.DataCell>
                <OppgavetypeTag oppgavetype={oppgave.type} />
              </Table.DataCell>
              <Table.DataCell>{oppgave.merknad}</Table.DataCell>
              <Table.DataCell>{oppgave.status ? OPPGAVESTATUSFILTER[oppgave.status] : 'Ukjent status'}</Table.DataCell>
              <Table.DataCell>
                <VelgSaksbehandler saksbehandlereIEnhet={saksbehandlereIEnheter} oppgave={oppgave} />
              </Table.DataCell>
              <Table.DataCell>
                <HandlingerForOppgave oppgave={oppgave} revurderingsaarsaker={new RevurderingsaarsakerDefault()} />
              </Table.DataCell>
            </Table.Row>
          ))}
      </Table.Body>
    </Table>
  )
}
