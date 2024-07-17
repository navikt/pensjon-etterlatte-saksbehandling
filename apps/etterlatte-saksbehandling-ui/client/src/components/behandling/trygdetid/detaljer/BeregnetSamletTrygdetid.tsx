import React from 'react'
import { Table } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetidResultat } from '~shared/api/trygdetid'

interface Props {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetidResultat
}

export const BeregnetSamletTrygdetid = ({ beregnetTrygdetid }: Props) => {
  const opprinneligYrkesskadeTekst = beregnetTrygdetid.beregnetSamletTrygdetidNorge
    ? ' - opprinnelig beregnet verdi ' + beregnetTrygdetid.beregnetSamletTrygdetidNorge
    : ''

  const yrkesskadeTekst = beregnetTrygdetid.yrkesskade
    ? ' (justert for yrkesskade' + opprinneligYrkesskadeTekst + ')'
    : ''

  return (
    <Table size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Samlet trygdetid</Table.HeaderCell>
          <Table.HeaderCell />
          <Table.HeaderCell />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.DataCell>Samlet trygdetid for nasjonal beregning</Table.DataCell>
          <Table.DataCell colSpan={2}>
            {beregnetTrygdetid.samletTrygdetidNorge}
            {yrkesskadeTekst}
          </Table.DataCell>
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Samlet trygdetid for beregning av teoretisk beløp</Table.DataCell>
          <Table.DataCell>{beregnetTrygdetid.samletTrygdetidTeoretisk}</Table.DataCell>
          <Table.DataCell />
        </Table.Row>
        <Table.Row>
          <Table.DataCell>Prorata brøk</Table.DataCell>
          <Table.DataCell>
            {beregnetTrygdetid.prorataBroek ? (
              <span>
                {beregnetTrygdetid.prorataBroek.teller}/{beregnetTrygdetid.prorataBroek.nevner}
              </span>
            ) : (
              '-'
            )}
          </Table.DataCell>
          <Table.DataCell />
        </Table.Row>
      </Table.Body>
    </Table>
  )
}
