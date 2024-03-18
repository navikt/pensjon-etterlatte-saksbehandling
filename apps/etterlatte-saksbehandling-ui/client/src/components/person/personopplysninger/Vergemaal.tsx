import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { PersonGroupIcon } from '@navikt/aksel-icons'
import { CopyButton, Heading, Table } from '@navikt/ds-react'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'
import { lowerCase, upperFirst } from 'lodash'

export const Vergemaal = ({
  vergemaalEllerFremtidsfullmakt,
}: {
  vergemaalEllerFremtidsfullmakt?: VergemaalEllerFremtidsfullmakt[]
}): ReactNode => {
  return (
    <Personopplysning heading="Vergemål" icon={<PersonGroupIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Verge</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Omfang</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Type</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Embete</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {vergemaalEllerFremtidsfullmakt && vergemaalEllerFremtidsfullmakt.length > 0 ? (
            <>
              {vergemaalEllerFremtidsfullmakt.map((verge: VergemaalEllerFremtidsfullmakt, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    {verge.vergeEllerFullmektig.motpartsPersonident && verge.vergeEllerFullmektig.navn}
                  </Table.DataCell>
                  <Table.DataCell>
                    {!!verge.vergeEllerFullmektig.motpartsPersonident && (
                      <CopyButton
                        copyText={verge.vergeEllerFullmektig.motpartsPersonident}
                        text={verge.vergeEllerFullmektig.motpartsPersonident}
                        size="small"
                        iconPosition="right"
                      />
                    )}
                  </Table.DataCell>
                  <Table.DataCell>
                    {!!verge.vergeEllerFullmektig.omfang && verge.vergeEllerFullmektig.omfang}
                  </Table.DataCell>
                  <Table.DataCell>{!!verge.type && upperFirst(lowerCase(verge.type))}</Table.DataCell>
                  <Table.DataCell>{!!verge.embete && verge.embete}</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell>
                <Heading size="small">Ingen vergemål</Heading>
              </Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
