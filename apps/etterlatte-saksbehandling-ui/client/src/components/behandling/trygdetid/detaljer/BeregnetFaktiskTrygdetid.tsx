import React from 'react'
import { Table } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetidResultat } from '~shared/api/trygdetid'
import { formaterBeregnetTrygdetid, TrygdetidTabell } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetidResultat
}
export const BeregnetFaktiskTrygdetid: React.FC<Props> = ({ beregnetTrygdetid }) => (
  <TrygdetidTabell>
    <Table size={'small'}>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Faktisk trygdetid</Table.HeaderCell>
          <Table.HeaderCell>I Norge</Table.HeaderCell>
          <Table.HeaderCell>I Norge og avtaleland</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.DataCell>Faktisk trygdetid (inkl. tid fom. 67 år)</Table.DataCell>
          <Table.DataCell>{formaterBeregnetTrygdetid(beregnetTrygdetid.faktiskTrygdetidNorge?.periode)}</Table.DataCell>
          <Table.DataCell>
            {formaterBeregnetTrygdetid(beregnetTrygdetid.faktiskTrygdetidTeoretisk?.periode)}
          </Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Faktisk trygdetid i måneder (inkl. tid fom. 67 år)</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.faktiskTrygdetidNorge?.antallMaaneder}</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.faktiskTrygdetidTeoretisk?.antallMaaneder}</Table.DataCell>
        </Table.Row>
      </Table.Body>
    </Table>
  </TrygdetidTabell>
)
