import { ISak } from '~shared/types/sak'
import { Box, Heading, Table } from '@navikt/ds-react'
import { formaterSakstype } from '~utils/formatering/formatering'
import React from 'react'

export const SakOverfoeringDetailjer = ({ fra, til }: { fra: ISak; til: ISak }) => (
  <Box borderWidth="1" borderColor="border-neutral-subtle" padding="space-4">
    <Heading size="xsmall" spacing>
      Flyttedetaljer
    </Heading>

    <Table size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          <Table.HeaderCell>Fra</Table.HeaderCell>
          <Table.HeaderCell>Til</Table.HeaderCell>
        </Table.Row>
      </Table.Header>

      <Table.Body>
        <Table.Row>
          <Table.HeaderCell>Sakid</Table.HeaderCell>
          <Table.DataCell>{fra.id}</Table.DataCell>
          <Table.DataCell>{til.id}</Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.HeaderCell>Ident</Table.HeaderCell>
          <Table.DataCell>{fra.ident}</Table.DataCell>
          <Table.DataCell>{til.ident}</Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.HeaderCell>Sakstype</Table.HeaderCell>
          <Table.DataCell>{formaterSakstype(fra.sakType)}</Table.DataCell>
          <Table.DataCell>{formaterSakstype(til.sakType)}</Table.DataCell>
        </Table.Row>
      </Table.Body>
    </Table>
  </Box>
)
