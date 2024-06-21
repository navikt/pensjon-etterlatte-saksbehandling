import React, { ReactNode, useEffect, useState } from 'react'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveSaksbehandler, Oppgavetype } from '~shared/types/oppgave'
import { Alert, HStack, Table } from '@navikt/ds-react'
import { formaterEnumTilLesbarString, formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import { OppgavetypeTag } from '~shared/tags/OppgavetypeTag'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { HandlingerForOppgave } from '~components/oppgavebenk/components/HandlingerForOppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { OppgaveValg } from '~components/person/sakOgBehandling/SakOversikt'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveHandlinger'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { OppgavestatusTag } from '~shared/tags/OppgavestatusTag'
import styled from 'styled-components'

export const ForenkletOppgaverTable = ({
  oppgaver,
  oppgaveValg,
}: {
  oppgaver: OppgaveDTO[]
  oppgaveValg: OppgaveValg
}): ReactNode => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const filtrerOppgaverPaaOppgaveValg = (): OppgaveDTO[] => {
    switch (oppgaveValg) {
      case OppgaveValg.AKTIVE:
        return sorterOppgaverEtterOpprettet([...oppgaver].filter((oppgave) => erOppgaveRedigerbar(oppgave.status)))
      case OppgaveValg.FERDIGSTILTE:
        return sorterOppgaverEtterOpprettet([...oppgaver].filter((oppgave) => !erOppgaveRedigerbar(oppgave.status)))
    }
  }

  const [filtrerteOppgaver, setFiltrerteOppgaver] = useState<OppgaveDTO[]>(filtrerOppgaverPaaOppgaveValg())

  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  const [, saksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)

  const oppdaterSaksbehandlerTildeling = (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) =>
    setFiltrerteOppgaver(finnOgOppdaterSaksbehandlerTildeling(filtrerteOppgaver, oppgave.id, saksbehandler))

  useEffect(() => {
    setFiltrerteOppgaver(filtrerOppgaverPaaOppgaveValg())
  }, [oppgaveValg])

  useEffect(() => {
    if (!!innloggetSaksbehandler.enheter.length) {
      saksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, setSaksbehandlereIEnheter)
    }
  }, [])

  return !!filtrerteOppgaver?.length ? (
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
        {filtrerteOppgaver.map((oppgave: OppgaveDTO) => (
          <Table.Row key={oppgave.id}>
            <Table.DataCell>{formaterStringDato(oppgave.opprettet)}</Table.DataCell>
            <Table.DataCell>
              <FristWrapper dato={oppgave.frist} />
            </Table.DataCell>
            <Table.DataCell>
              <HStack align="center">
                <SakTypeTag sakType={oppgave.sakType} kort />
              </HStack>
            </Table.DataCell>
            <Table.DataCell>
              <OppgavetypeTag oppgavetype={oppgave.type} />
            </Table.DataCell>
            <Table.DataCell>{oppgave.merknad}</Table.DataCell>
            <Table.DataCell>
              <OppgavestatusTag oppgavestatus={oppgave.status} />
            </Table.DataCell>
            <Table.DataCell>
              <VelgSaksbehandler
                saksbehandlereIEnhet={saksbehandlereIEnheter}
                oppgave={oppgave}
                oppdaterTildeling={oppdaterSaksbehandlerTildeling}
              />
            </Table.DataCell>
            <HandlingerDataCell>
              {oppgave.type !== Oppgavetype.VURDER_KONSEKVENS && <HandlingerForOppgave oppgave={oppgave} />}
            </HandlingerDataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  ) : (
    <Alert variant="info" inline>
      Ingen {formaterEnumTilLesbarString(oppgaveValg).toLowerCase()} oppgaver på sak
    </Alert>
  )
}

const HandlingerDataCell = styled(Table.DataCell)`
  min-width: 13rem;
`
