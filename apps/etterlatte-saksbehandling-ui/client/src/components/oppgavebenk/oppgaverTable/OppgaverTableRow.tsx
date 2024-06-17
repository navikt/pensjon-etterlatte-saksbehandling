import React, { ReactNode } from 'react'
import { HStack, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import SaksoversiktLenke from '~components/oppgavebenk/components/SaksoversiktLenke'
import { OppgavetypeTag } from '~components/oppgavebenk/components/tags/Tags'
import { HandlingerForOppgave } from '~components/oppgavebenk/components/HandlingerForOppgave'
import { FristHandlinger } from '~components/oppgavebenk/frist/FristHandlinger'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveSaksbehandler } from '~shared/types/oppgave'
import { SakTypeTag } from '~components/oppgavebenk/components/tags/SakTypeTag'
import { OppgavestatusTag } from '~components/oppgavebenk/components/tags/OppgavestatusTag'
import styled from 'styled-components'

interface Props {
  oppgave: OppgaveDTO
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
  oppdaterFrist?: (id: string, nyfrist: string) => void
}

export const OppgaverTableRow = ({
  oppgave,
  saksbehandlereIEnhet,
  oppdaterTildeling,
  oppdaterFrist,
}: Props): ReactNode => (
  <Table.Row>
    <Table.DataCell>{oppgave.sakId}</Table.DataCell>
    <Table.DataCell>{formaterStringDato(oppgave.opprettet)}</Table.DataCell>
    <Table.DataCell>
      {oppdaterFrist ? (
        <FristHandlinger
          orginalFrist={oppgave.frist}
          oppgaveId={oppgave.id}
          oppdaterFrist={oppdaterFrist}
          erRedigerbar={erOppgaveRedigerbar(oppgave.status)}
        />
      ) : (
        <FristWrapper dato={oppgave.frist} />
      )}
    </Table.DataCell>
    <Table.DataCell>{oppgave.fnr ? <SaksoversiktLenke fnr={oppgave.fnr} /> : 'Mangler'}</Table.DataCell>
    <Table.DataCell>
      <HStack align="center">
        <SakTypeTag sakType={oppgave.sakType} kort />
      </HStack>
    </Table.DataCell>
    <Table.DataCell>
      {oppgave.type ? <OppgavetypeTag oppgavetype={oppgave.type} /> : <div>oppgaveid {oppgave.id}</div>}
    </Table.DataCell>
    <Table.DataCell>{oppgave.merknad}</Table.DataCell>
    <Table.DataCell>
      <OppgavestatusTag oppgavestatus={oppgave.status} />
    </Table.DataCell>
    <Table.DataCell>{oppgave.enhet}</Table.DataCell>
    <Table.DataCell>
      <VelgSaksbehandler
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppdaterTildeling={oppdaterTildeling}
        oppgave={oppgave}
        key={`${oppgave.id}${oppgave.saksbehandler?.ident}`}
        // For å trigge unmount og resetting av state når man bytte liste. Se https://react.dev/learn/preserving-and-resetting-state#same-component-at-the-same-position-preserves-state
      />
    </Table.DataCell>
    <HandlingerDataCell>
      <HandlingerForOppgave oppgave={oppgave} />
    </HandlingerDataCell>
  </Table.Row>
)

const HandlingerDataCell = styled(Table.DataCell)`
  min-width: 13rem;
`
