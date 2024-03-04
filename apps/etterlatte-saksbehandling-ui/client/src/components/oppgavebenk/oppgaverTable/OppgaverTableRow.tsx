import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import SaksoversiktLenke from '~components/oppgavebenk/components/SaksoversiktLenke'
import { OppgavetypeTag, SaktypeTag } from '~components/oppgavebenk/components/Tags'
import { HandlingerForOppgave } from '~components/oppgavebenk/components/HandlingerForOppgave'
import { FristHandlinger } from '~components/oppgavebenk/frist/FristHandlinger'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { OPPGAVESTATUSFILTER } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'

interface Props {
  oppgave: OppgaveDTO
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  oppdaterFrist?: (id: string, nyfrist: string, versjon: number | null) => void
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
}

export const OppgaverTableRow = ({
  oppgave,
  saksbehandlereIEnhet,
  oppdaterTildeling,
  oppdaterFrist,
  revurderingsaarsaker,
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
          oppgaveVersjon={oppgave.versjon}
          type={oppgave.type}
        />
      ) : (
        <FristWrapper dato={oppgave.frist} />
      )}
    </Table.DataCell>
    <Table.DataCell>{oppgave.fnr ? <SaksoversiktLenke fnr={oppgave.fnr} /> : 'Mangler'}</Table.DataCell>
    <Table.DataCell>
      {oppgave.type ? <OppgavetypeTag oppgavetype={oppgave.type} /> : <div>oppgaveid {oppgave.id}</div>}
    </Table.DataCell>
    <Table.DataCell>{oppgave.sakType && <SaktypeTag sakType={oppgave.sakType} />}</Table.DataCell>
    <Table.DataCell>{oppgave.merknad}</Table.DataCell>
    <Table.DataCell>{oppgave.status ? OPPGAVESTATUSFILTER[oppgave.status] : 'Ukjent'}</Table.DataCell>
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
    <Table.DataCell>
      <HandlingerForOppgave oppgave={oppgave} revurderingsaarsaker={revurderingsaarsaker} />
    </Table.DataCell>
  </Table.Row>
)
