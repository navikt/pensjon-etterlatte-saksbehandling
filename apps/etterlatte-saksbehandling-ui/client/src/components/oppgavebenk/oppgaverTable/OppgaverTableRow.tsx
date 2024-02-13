import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'
import { erOppgaveRedigerbar, OppgaveDTO, Saksbehandler } from '~shared/api/oppgaver'
import { formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/oppgavebenk/FristWrapper'
import SaksoversiktLenke from '~components/oppgavebenk/SaksoversiktLenke'
import { OppgavetypeTag, SaktypeTag } from '~components/oppgavebenk/Tags'
import { HandlingerForOppgave } from '~components/oppgavebenk/HandlingerForOppgave'
import { FristHandlinger } from '~components/oppgavebenk/FristHandlinger'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { OPPGAVESTATUSFILTER } from '~components/oppgavebenk/filter/oppgavelistafiltre'

interface Props {
  oppgave: OppgaveDTO
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: string | null, versjon: number | null) => void
  erMinOppgaveListe: boolean
  oppdaterFrist: (id: string, nyfrist: string, versjon: number | null) => void
}

export const OppgaverTableRow = ({
  oppgave,
  saksbehandlereIEnhet,
  oppdaterTildeling,
  erMinOppgaveListe,
  oppdaterFrist,
}: Props): ReactNode => {
  return (
    <Table.Row>
      <Table.HeaderCell>{formaterStringDato(oppgave.opprettet)}</Table.HeaderCell>
      <Table.DataCell>
        {erMinOppgaveListe ? (
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
          saksbehandler={{
            saksbehandlerIdent: oppgave.saksbehandlerIdent,
            saksbehandlerNavn: oppgave.saksbehandlerNavn,
          }}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          oppdaterTildeling={oppdaterTildeling}
          oppgave={oppgave}
        />
      </Table.DataCell>
      <Table.DataCell>
        <HandlingerForOppgave oppgave={oppgave} />
      </Table.DataCell>
    </Table.Row>
  )
}
