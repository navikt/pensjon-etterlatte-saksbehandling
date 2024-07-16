import React from 'react'
import { Table } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetidResultat } from '~shared/api/trygdetid'
import { formaterBeregnetTrygdetid } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetidResultat
}
export const BeregnetFremtidigTrygdetid = ({ beregnetTrygdetid }: Props) => (
  <Table size="small">
    <Table.Header>
      <Table.Row>
        <Table.HeaderCell>Fremtidig trygdetid</Table.HeaderCell>
        <Table.HeaderCell>I Norge</Table.HeaderCell>
        <Table.HeaderCell>I Norge og avtaleland</Table.HeaderCell>
      </Table.Row>
    </Table.Header>
    <Table.Body>
      <Table.Row>
        <Table.DataCell>Opptjeningstid i måneder</Table.DataCell>
        <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidNorge?.opptjeningstidIMaaneder}</Table.DataCell>
        <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidTeoretisk?.opptjeningstidIMaaneder}</Table.DataCell>
      </Table.Row>
      <Table.Row>
        <Table.DataCell>Faktisk trygdetid mindre enn 4/5 av opptjeningstiden</Table.DataCell>
        <Table.DataCell>
          {beregnetTrygdetid.fremtidigTrygdetidNorge && (
            <span>
              {beregnetTrygdetid.fremtidigTrygdetidNorge?.mindreEnnFireFemtedelerAvOpptjeningstiden ? 'Ja' : 'Nei'}
            </span>
          )}
        </Table.DataCell>
        <Table.DataCell>
          {beregnetTrygdetid.fremtidigTrygdetidTeoretisk && (
            <span>
              {beregnetTrygdetid.fremtidigTrygdetidTeoretisk?.mindreEnnFireFemtedelerAvOpptjeningstiden ? 'Ja' : 'Nei'}
            </span>
          )}
        </Table.DataCell>
      </Table.Row>
      <Table.Row>
        <Table.DataCell>Fremtidig trygdetid</Table.DataCell>
        <Table.DataCell>{formaterBeregnetTrygdetid(beregnetTrygdetid.fremtidigTrygdetidNorge?.periode)}</Table.DataCell>
        <Table.DataCell>
          {formaterBeregnetTrygdetid(beregnetTrygdetid.fremtidigTrygdetidTeoretisk?.periode)}
        </Table.DataCell>
      </Table.Row>
      <Table.Row>
        <Table.DataCell>Fremtidig trygdetid i måneder</Table.DataCell>
        <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidNorge?.antallMaaneder}</Table.DataCell>
        <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidTeoretisk?.antallMaaneder}</Table.DataCell>
      </Table.Row>
    </Table.Body>
  </Table>
)
