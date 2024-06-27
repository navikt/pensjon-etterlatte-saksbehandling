import { formaterOppgavetype, formaterStatus, GosysOppgave } from '~shared/types/Gosys'
import { Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { VelgSaksbehandler } from '~components/oppgavebenk/gosys/VelgSaksbehandler'
import { GosysOppgaveModal } from '~components/oppgavebenk/oppgaveModal/GosysOppgaveModal'
import React, { useState } from 'react'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { GosysBrukerWrapper } from '~components/oppgavebenk/gosys/GosysBrukerWrapper'
import { GosysTemaTag } from '~shared/tags/GosysTemaTag'

export const GosysOppgaveRow = (props: { oppgave: GosysOppgave; saksbehandlereIEnhet: Array<Saksbehandler> }) => {
  const [oppgave, setOppgave] = useState(props.oppgave)

  return (
    <Table.Row>
      <Table.DataCell>{formaterDato(oppgave.opprettet)}</Table.DataCell>
      <Table.DataCell>{oppgave.frist ? formaterDato(oppgave.frist) : 'Mangler'}</Table.DataCell>
      <Table.DataCell>
        <GosysBrukerWrapper bruker={oppgave.bruker} />
      </Table.DataCell>
      <Table.DataCell>{formaterOppgavetype(oppgave.oppgavetype)}</Table.DataCell>
      <Table.DataCell>
        <GosysTemaTag tema={oppgave.tema} />
      </Table.DataCell>
      <Table.DataCell>{oppgave.beskrivelse}</Table.DataCell>
      <Table.DataCell>{formaterStatus(oppgave.status)}</Table.DataCell>
      <Table.DataCell>{oppgave.enhet}</Table.DataCell>
      <Table.DataCell>
        <VelgSaksbehandler
          saksbehandlereIEnhet={props.saksbehandlereIEnhet}
          oppgave={oppgave}
          oppdaterTildeling={(saksbehandler) => setOppgave({ ...oppgave, saksbehandler })}
        />
      </Table.DataCell>
      <Table.DataCell>
        <GosysOppgaveModal oppgave={oppgave} />
      </Table.DataCell>
    </Table.Row>
  )
}
