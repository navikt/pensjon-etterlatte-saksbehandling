import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'
import { OppgaverTableHeader } from '~components/nyoppgavebenk/oppgaverTable/OppgaverTableHeader'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { OppgaverTableRow } from '~components/nyoppgavebenk/oppgaverTable/OppgaverTableRow'

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
}

export const OppgaverTable = ({ oppgaver, oppdaterTildeling }: Props): ReactNode => {
  return (
    <Table>
      <OppgaverTableHeader />
      <Table.Body>
        {oppgaver &&
          oppgaver.map((oppgave: OppgaveDTO) => {
            return <OppgaverTableRow key={oppgave.id} oppgave={oppgave} oppdaterTildeling={oppdaterTildeling} />
          })}
      </Table.Body>
    </Table>
  )
}
