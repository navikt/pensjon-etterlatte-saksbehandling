import React from 'react'
import { InstitusjonsoppholdGrunnlagData } from '~shared/types/Beregning'
import { Table } from '@navikt/ds-react'

interface Props {
  institusjonsopphold: InstitusjonsoppholdGrunnlagData
}

export const InstitusjonsoppholdBeregningsgrunnlagTable = ({ institusjonsopphold }: Props) => {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Reduksjon</Table.HeaderCell>
          <Table.HeaderCell scope="col" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {!!institusjonsopphold?.length ? (
          <Table.ExpandableRow content={<></>} />
        ) : (
          <Table.Row>
            <Table.DataCell colSpan={5}>Ingen perioder for institusjonsopphold</Table.DataCell>
          </Table.Row>
        )}
      </Table.Body>
    </Table>
  )
}
