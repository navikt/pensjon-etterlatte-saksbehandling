import { JournalpostSak } from '~shared/types/Journalpost'
import { ISak, SakType } from '~shared/types/sak'
import { Alert, Button, Heading, Table } from '@navikt/ds-react'
import { formaterSakstype } from '~utils/formattering'
import React from 'react'

const temaFraSakstype = (sakstype: SakType): string => {
  switch (sakstype) {
    case SakType.BARNEPENSJON:
      return 'EYB'
    case SakType.OMSTILLINGSSTOENAD:
      return 'EYO'
  }
}

export const EndreSak = ({
  fagsak,
  gjennySak,
  kobleTilSak,
}: {
  fagsak?: JournalpostSak
  gjennySak: ISak
  kobleTilSak: (sak: JournalpostSak) => void
}) => {
  const konverterOgKobleTilSak = () => {
    kobleTilSak({
      sakstype: 'FAGSAK',
      fagsakId: gjennySak.id.toString(),
      fagsaksystem: 'EY',
      tema: temaFraSakstype(gjennySak.sakType),
    })
  }

  return (
    <div>
      <Heading size="small" spacing>
        Sak
      </Heading>

      {!fagsak || fagsak.fagsakId !== gjennySak.id.toString() ? (
        <Alert variant="warning">
          Journalposten er ikke tilknyttet sak i Gjenny. Du kan koble til brukeren sin sak {gjennySak.id}{' '}
          {formaterSakstype(gjennySak.sakType)}.
          <br />
          <Button variant="secondary" size="small" onClick={konverterOgKobleTilSak}>
            Koble til sak
          </Button>
        </Alert>
      ) : (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>SakID</Table.HeaderCell>
              <Table.HeaderCell>Sakstype</Table.HeaderCell>
              <Table.HeaderCell>Fagsystem</Table.HeaderCell>
              <Table.HeaderCell>Tema</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.DataCell>{fagsak?.fagsakId}</Table.DataCell>
              <Table.DataCell>{fagsak?.sakstype}</Table.DataCell>
              <Table.DataCell>{fagsak?.fagsaksystem}</Table.DataCell>
              <Table.DataCell>{fagsak?.tema}</Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      )}
    </div>
  )
}
