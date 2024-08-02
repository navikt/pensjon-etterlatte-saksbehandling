import React, { ReactNode } from 'react'
import { HStack, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import SaksoversiktLenke from '~components/oppgavebenk/components/SaksoversiktLenke'
import { OppgavetypeTag } from '~shared/tags/OppgavetypeTag'
import { HandlingerForOppgave } from '~components/oppgavebenk/components/HandlingerForOppgave'
import { FristHandlinger } from '~components/oppgavebenk/frist/FristHandlinger'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { OppgavestatusTag } from '~shared/tags/OppgavestatusTag'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'
import styled from 'styled-components'

interface Props {
  oppgave: OppgaveDTO
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
  oppdaterFrist?: (id: string, nyfrist: string) => void
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const OppgaverTableRow = ({
  oppgave,
  saksbehandlereIEnhet,
  oppdaterTildeling,
  oppdaterFrist,
  oppdaterStatus,
}: Props): ReactNode => (
  <Table.Row>
    <Table.DataCell>
      <SaksoversiktLenke sakId={oppgave.sakId} />
    </Table.DataCell>
    <Table.DataCell>{formaterDato(oppgave.opprettet)}</Table.DataCell>
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
    <Table.DataCell>{oppgave.fnr ? oppgave.fnr : 'Mangler'}</Table.DataCell>
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
      <HandlingerForOppgave oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
    </HandlingerDataCell>
  </Table.Row>
)

const HandlingerDataCell = styled(Table.DataCell)`
  min-width: 13rem;
`
