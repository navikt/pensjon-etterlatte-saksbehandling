import React from 'react'
import { Table } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetid } from '~shared/api/trygdetid'
import { formaterBeregnetTrygdetid, TrygdetidTabell } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
}
export const BeregnetFremtidigTrygdetid: React.FC<Props> = ({ beregnetTrygdetid }) => (
  <TrygdetidTabell>
    <Table size={'small'}>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell style={{ width: '400px' }}>Fremtidig trygdetid</Table.HeaderCell>
          <Table.HeaderCell style={{ width: '200px' }}>I Norge</Table.HeaderCell>
          <Table.HeaderCell style={{ width: '200px' }}>I Norge og avtaleland</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.DataCell>Opptjeningstid i måneder</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidNorge.opptjeningstidIMaaneder}</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidTeoretiskBeloep.opptjeningstidIMaaneder}</Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Faktisk trygdetid mindre enn 4/5 av opptjeningstiden</Table.DataCell>
          <Table.DataCell>
            {beregnetTrygdetid.fremtidigTrygdetidNorge.mindreEnnFireFemtedelerAvOpptjeningstiden ? 'Ja' : 'Nei'}
          </Table.DataCell>
          <Table.DataCell>
            {beregnetTrygdetid.fremtidigTrygdetidTeoretiskBeloep.mindreEnnFireFemtedelerAvOpptjeningstiden
              ? 'Ja'
              : 'Nei'}
          </Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Fremtidig trygdetid</Table.DataCell>
          <Table.DataCell>{formaterBeregnetTrygdetid(beregnetTrygdetid.fremtidigTrygdetidNorge.verdi)}</Table.DataCell>
          <Table.DataCell>
            {formaterBeregnetTrygdetid(beregnetTrygdetid.fremtidigTrygdetidTeoretiskBeloep.verdi)}
          </Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Fremtidig trygdetid i måneder</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidNorge.antallMaaneder}</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidTeoretiskBeloep.antallMaaneder}</Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Nordisk konvensjon artikkel 10</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.fremtidigTrygdetidNorge.nordiskKonvensjon ? 'Ja' : 'Nei'}</Table.DataCell>
          <Table.DataCell>-</Table.DataCell>
        </Table.Row>
      </Table.Body>
    </Table>
  </TrygdetidTabell>
)
