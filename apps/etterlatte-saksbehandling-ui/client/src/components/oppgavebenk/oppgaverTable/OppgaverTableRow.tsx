import React, { ReactNode } from 'react'
import { HStack, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { OppgavetypeTag } from '~shared/tags/OppgavetypeTag'
import { HandlingerForOppgave } from '~components/oppgavebenk/components/HandlingerForOppgave'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { OppgavestatusTag } from '~shared/tags/OppgavestatusTag'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'
import styled from 'styled-components'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { OppgaveFrist } from '~components/oppgavebenk/frist/OppgaveFrist'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

interface Props {
  oppgave: OppgaveDTO
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
  oppdaterFrist: (id: string, nyfrist: string) => void
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const OppgaverTableRow = ({
  oppgave,
  saksbehandlereIEnhet,
  oppdaterTildeling,
  oppdaterFrist,
  oppdaterStatus,
}: Props): ReactNode => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  let kanSeDetaljertInfo = true
  const spesialEnhet = oppgave.enhet === '2103' || oppgave.enhet === '4883'
  if (spesialEnhet) {
    kanSeDetaljertInfo = innloggetSaksbehandler.skriveEnheter.some((e) => e === oppgave.enhet)
  }

  return (
    <Table.Row>
      <Table.DataCell>{oppgave.sakId}</Table.DataCell>
      <Table.DataCell>{formaterDato(oppgave.opprettet)}</Table.DataCell>
      <Table.DataCell>
        <OppgaveFrist oppgave={oppgave} oppdaterFrist={oppdaterFrist} />
      </Table.DataCell>
      <Table.DataCell>
        {kanSeDetaljertInfo ? (
          ''
        ) : (
          <>{oppgave.fnr ? <PersonLink fnr={oppgave.fnr}>{oppgave.fnr}</PersonLink> : 'Mangler'}</>
        )}
      </Table.DataCell>
      <Table.DataCell>
        <HStack align="center">
          <SakTypeTag sakType={oppgave.sakType} kort />
        </HStack>
      </Table.DataCell>
      <Table.DataCell>
        {oppgave.type ? <OppgavetypeTag oppgavetype={oppgave.type} /> : <div>oppgaveid {oppgave.id}</div>}
      </Table.DataCell>
      <Table.DataCell>{kanSeDetaljertInfo && oppgave.merknad}</Table.DataCell>
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
}

const HandlingerDataCell = styled(Table.DataCell)`
  min-width: 13rem;
`
