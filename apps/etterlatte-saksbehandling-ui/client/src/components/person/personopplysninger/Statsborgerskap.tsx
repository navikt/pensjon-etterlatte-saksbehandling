import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { PassportIcon } from '@navikt/aksel-icons'
import { Statsborgerskap as PdlStatsborgerskap } from '~shared/types/Person'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'

export const Statsborgerskap = ({
  statsborgerskap,
  pdlStatsborgerskap,
}: {
  statsborgerskap?: string
  pdlStatsborgerskap?: PdlStatsborgerskap[]
}): ReactNode => {
  return (
    <Personopplysning heading="Statsborgerskap" icon={<PassportIcon height="2rem" width="2rem" />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Land</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Til</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Personstatus</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {pdlStatsborgerskap && pdlStatsborgerskap.length >= 0 ? (
            pdlStatsborgerskap.map((borgerskap: PdlStatsborgerskap, index: number) => (
              <Table.Row key={index}>
                <Table.DataCell>{borgerskap.land}</Table.DataCell>
                <Table.DataCell>
                  {!!borgerskap.gyldigFraOgMed ? formaterStringDato(borgerskap.gyldigFraOgMed) : ''}
                </Table.DataCell>
                <Table.DataCell>
                  {!!borgerskap.gyldigTilOgMed ? formaterStringDato(borgerskap.gyldigTilOgMed) : ''}
                </Table.DataCell>
                <Table.DataCell>{borgerskap.land === statsborgerskap && 'Bosatt'}</Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell>{statsborgerskap}</Table.DataCell>
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
