import React, { ReactNode, useState } from 'react'
import { Table } from '@navikt/ds-react'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/api/oppgaver'
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
  oppgaver: ReadonlyArray<OppgaveDTO>
  alleOppgaver: Array<OppgaveDTO>
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  erMinOppgaveListe: boolean
  hentOppgaver: () => void
}

export const OppgaverTableRow = ({
  oppgave,
  alleOppgaver,
  oppdaterTildeling,
  erMinOppgaveListe,
  hentOppgaver,
}: Props): ReactNode => {
  const [saksbehandlere] = useState<Array<string>>(
    Array.from(
      new Set(
        alleOppgaver.map((oppgave) => oppgave.saksbehandlerNavn).filter((s): s is Exclude<typeof s, null> => s !== null)
      )
    )
  )

  return (
    <Table.Row>
      <Table.HeaderCell>{formaterStringDato(oppgave.opprettet)}</Table.HeaderCell>
      <Table.DataCell>
        {erMinOppgaveListe ? (
          <FristHandlinger
            orginalFrist={oppgave.frist}
            oppgaveId={oppgave.id}
            hentOppgaver={hentOppgaver}
            erRedigerbar={erOppgaveRedigerbar(oppgave.status)}
            oppgaveVersjon={oppgave.versjon}
            type={oppgave.type}
          />
        ) : (
          <FristWrapper dato={oppgave.frist} />
        )}
      </Table.DataCell>
      <Table.DataCell>
        <SaksoversiktLenke fnr={oppgave.fnr} />
      </Table.DataCell>
      <Table.DataCell>
        {oppgave.type ? <OppgavetypeTag oppgavetype={oppgave.type} /> : <div>oppgaveid {oppgave.id}</div>}
      </Table.DataCell>
      <Table.DataCell>{oppgave.sakType && <SaktypeTag sakType={oppgave.sakType} />}</Table.DataCell>
      <Table.DataCell>{oppgave.merknad}</Table.DataCell>
      <Table.DataCell>{oppgave.status ? OPPGAVESTATUSFILTER[oppgave.status] : 'Ukjent'}</Table.DataCell>
      <Table.DataCell>{oppgave.enhet}</Table.DataCell>
      <Table.DataCell>
        <VelgSaksbehandler
          saksbehandlere={saksbehandlere}
          saksbehandlerNavn={oppgave.saksbehandlerNavn}
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
