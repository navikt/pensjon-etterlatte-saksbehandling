import { formaterOppgavetype, formaterStatus, GosysOppgave } from '~shared/types/Gosys'
import { Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { VelgSaksbehandler } from '~components/oppgavebenk/gosys/VelgSaksbehandler'
import { GosysOppgaveModal } from '~components/oppgavebenk/oppgaveModal/GosysOppgaveModal'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { GosysBrukerWrapper } from '~components/oppgavebenk/gosys/GosysBrukerWrapper'
import { GosysTemaTag } from '~shared/tags/GosysTemaTag'
import { OppdatertOppgaveversjonResponseDto } from '~shared/api/gosys'
import { OppgaveSaksbehandler } from '~shared/types/oppgave'

export const GosysOppgaveRow = (props: {
  oppgave: GosysOppgave
  saksbehandlereIEnhet: Array<Saksbehandler>
  skjulBruker?: boolean
  oppdaterOppgaveTildeling: (
    oppgaveId: number,
    versjonDto: OppdatertOppgaveversjonResponseDto,
    saksbehandler?: OppgaveSaksbehandler
  ) => void
}) => {
  const oppgave = props.oppgave
  return (
    <Table.Row>
      <Table.DataCell>{formaterDato(oppgave.opprettet)}</Table.DataCell>
      <Table.DataCell>{oppgave.frist ? formaterDato(oppgave.frist) : 'Mangler'}</Table.DataCell>
      {!props.skjulBruker && (
        <Table.DataCell>
          <GosysBrukerWrapper bruker={oppgave.bruker} />
        </Table.DataCell>
      )}
      <Table.DataCell>
        <GosysTemaTag tema={oppgave.tema} />
      </Table.DataCell>
      <Table.DataCell>{formaterOppgavetype(oppgave.oppgavetype)}</Table.DataCell>
      <Table.DataCell>{oppgave.beskrivelse}</Table.DataCell>
      <Table.DataCell>{formaterStatus(oppgave.status)}</Table.DataCell>
      <Table.DataCell>{oppgave.enhet}</Table.DataCell>
      <Table.DataCell>
        <VelgSaksbehandler
          saksbehandlereIEnhet={props.saksbehandlereIEnhet}
          oppgave={oppgave}
          oppdaterTildeling={(oppgaveVersjonResponse, saksbehandler) =>
            props.oppdaterOppgaveTildeling(oppgave.id, oppgaveVersjonResponse, saksbehandler)
          }
        />
      </Table.DataCell>
      <Table.DataCell>
        <GosysOppgaveModal oppgave={oppgave} />
      </Table.DataCell>
    </Table.Row>
  )
}
