import React from 'react'
import { Table } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetid } from '~shared/api/trygdetid'
import { formaterBeregnetTrygdetid, TrygdetidTabell } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
}
export const BeregnetFaktiskTrygdetid: React.FC<Props> = ({ beregnetTrygdetid }) => (
  <TrygdetidTabell>
    <Table size={'small'}>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell style={{ width: '400px' }}>Faktisk trygdetid</Table.HeaderCell>
          <Table.HeaderCell style={{ width: '200px' }}>I Norge</Table.HeaderCell>
          <Table.HeaderCell style={{ width: '200px' }}>I Norge og avtaleland</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.DataCell>Faktisk trygdetid (inkl. tid fom. 67 år)</Table.DataCell>
          <Table.DataCell>{formaterBeregnetTrygdetid(beregnetTrygdetid.faktiskTrygdetidNorge.verdi)}</Table.DataCell>
          <Table.DataCell>
            {formaterBeregnetTrygdetid(beregnetTrygdetid.faktiskTrygdetidTeoretiskBeloep.verdi)}
          </Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Faktisk trygdetid i måneder (inkl. tid fom. 67 år)</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.faktiskTrygdetidNorge.antallMaaneder}</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.faktiskTrygdetidTeoretiskBeloep.antallMaaneder}</Table.DataCell>
        </Table.Row>
      </Table.Body>
    </Table>
  </TrygdetidTabell>
)
