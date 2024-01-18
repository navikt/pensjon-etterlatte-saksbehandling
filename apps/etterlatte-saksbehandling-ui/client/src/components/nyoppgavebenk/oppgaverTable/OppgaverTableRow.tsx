import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/api/oppgaver'
import { formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/nyoppgavebenk/FristWrapper'
import SaksoversiktLenke from '~components/nyoppgavebenk/SaksoversiktLenke'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/tildeling/RedigerSaksbehandler'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'

interface Props {
  oppgave: OppgaveDTO
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
}

export const OppgaverTableRow = ({ oppgave, oppdaterTildeling }: Props): ReactNode => {
  return (
    <Table.Row>
      <Table.HeaderCell>{formaterStringDato(oppgave.opprettet)}</Table.HeaderCell>
      <Table.DataCell>
        <FristWrapper dato={oppgave.frist} />
      </Table.DataCell>
      <Table.DataCell>
        <SaksoversiktLenke fnr={oppgave.fnr} />
      </Table.DataCell>
      <Table.DataCell>
        {oppgave.type ? <OppgavetypeTag oppgavetype={oppgave.type} /> : <div>oppgaveid {oppgave.id}</div>}
      </Table.DataCell>
      <Table.DataCell>{oppgave.sakType && <SaktypeTag sakType={oppgave.sakType} />}</Table.DataCell>
      <Table.DataCell>{oppgave.merknad}</Table.DataCell>
      <Table.DataCell>{oppgave.status ? oppgave.status : 'Ukjent'}</Table.DataCell>
      <Table.DataCell>{oppgave.enhet}</Table.DataCell>
      <Table.DataCell>
        <RedigerSaksbehandler
          saksbehandler={oppgave.saksbehandler}
          oppgaveId={oppgave.id}
          sakId={oppgave.sakId}
          oppdaterTildeling={oppdaterTildeling}
          erRedigerbar={erOppgaveRedigerbar(oppgave.status)}
          versjon={oppgave.versjon}
          type={oppgave.type}
        />
      </Table.DataCell>
      <Table.DataCell>
        <HandlingerForOppgave oppgave={oppgave} />
      </Table.DataCell>
    </Table.Row>
  )
}
