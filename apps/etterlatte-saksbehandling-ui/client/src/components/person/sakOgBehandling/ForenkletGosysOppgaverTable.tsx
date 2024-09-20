import React, { ReactNode, useEffect, useState } from 'react'
import { Alert, Table } from '@navikt/ds-react'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { useApiCall } from '~shared/hooks/useApiCall'
import { saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { OppgaveValg } from '~components/person/sakOgBehandling/SakOversikt'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { GosysOppgave } from '~shared/types/Gosys'
import { GosysOppgaveRow } from '~components/oppgavebenk/gosys/GosysOppgaveRow'

export const ForenkletGosysOppgaverTable = ({
  oppgaver,
  oppgaveValg,
}: {
  oppgaver: GosysOppgave[]
  oppgaveValg: OppgaveValg
}): ReactNode => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [saksbehandlereIEnhet, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])
  const [, saksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)

  useEffect(() => {
    if (!!innloggetSaksbehandler.enheter.length) {
      saksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, setSaksbehandlereIEnheter)
    }
  }, [])

  return !!oppgaver?.length ? (
    <Table zebraStripes size="small">
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Opprettet</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Frist</Table.ColumnHeader>
          <Table.HeaderCell scope="col">Tema</Table.HeaderCell>
          <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
          <Table.HeaderCell scope="col">Beskrivelse</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
          <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
          <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
          <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {oppgaver?.map((oppgave: GosysOppgave) => (
          <GosysOppgaveRow
            key={`gosysoppgave-${oppgave.id}`}
            oppgave={oppgave}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            skjulBruker={true}
          />
        ))}
      </Table.Body>
    </Table>
  ) : (
    <Alert variant="info" inline>
      Ingen {formaterEnumTilLesbarString(oppgaveValg).toLowerCase()} oppgaver på sak
    </Alert>
  )
}
