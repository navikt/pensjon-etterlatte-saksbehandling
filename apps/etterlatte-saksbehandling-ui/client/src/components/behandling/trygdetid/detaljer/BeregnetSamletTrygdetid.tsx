import React from 'react'
import { Table } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetid } from '~shared/api/trygdetid'
import { formaterBeregnetTrygdetid, TrygdetidTabell } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
}
export const BeregnetSamletTrygdetid: React.FC<Props> = ({ beregnetTrygdetid }) => (
  <TrygdetidTabell>
    <Table size={'small'}>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell style={{ width: '400px' }}>Samlet trygdetid</Table.HeaderCell>
          <Table.HeaderCell style={{ width: '200px' }} />
          <Table.HeaderCell style={{ width: '200px' }} />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.DataCell>Samlet trygdetid for nasjonal beregning</Table.DataCell>
          <Table.DataCell>{formaterBeregnetTrygdetid(beregnetTrygdetid.samletTrygdetidNasjonal)}</Table.DataCell>
          <Table.DataCell />
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Samlet trygdetid for beregning av teoretisk beløp</Table.DataCell>
          <Table.DataCell>{formaterBeregnetTrygdetid(beregnetTrygdetid.samletTrygdetidTeoretisk)}</Table.DataCell>
          <Table.DataCell />
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Pro rata brøk</Table.DataCell>
          <Table.DataCell>
            {beregnetTrygdetid.prorataBroek.teller}/{beregnetTrygdetid.prorataBroek.nevner}
          </Table.DataCell>
          <Table.DataCell />
        </Table.Row>
      </Table.Body>
    </Table>
  </TrygdetidTabell>
)
