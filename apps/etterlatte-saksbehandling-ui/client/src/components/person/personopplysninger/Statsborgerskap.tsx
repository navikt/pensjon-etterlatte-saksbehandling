import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { PassportIcon } from '@navikt/aksel-icons'
import { Statsborgerskap as PdlStatsborgerskap } from '~shared/types/Person'
import { Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { ILand } from '~shared/api/trygdetid'

export const Statsborgerskap = ({
  statsborgerskap,
  pdlStatsborgerskap,
  landListe,
}: {
  statsborgerskap?: string
  pdlStatsborgerskap?: PdlStatsborgerskap[]
  landListe: ILand[]
}): ReactNode => {
  return (
    <Personopplysning heading="Statsborgerskap" icon={<PassportIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Land</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Til</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!pdlStatsborgerskap?.length ? (
            pdlStatsborgerskap.map((borgerskap: PdlStatsborgerskap, index: number) => (
              <Table.Row key={index}>
                <Table.DataCell>{finnLandSomTekst(borgerskap.land, landListe)}</Table.DataCell>
                <Table.DataCell>
                  {!!borgerskap.gyldigFraOgMed ? formaterDato(borgerskap.gyldigFraOgMed) : ''}
                </Table.DataCell>
                <Table.DataCell>
                  {!!borgerskap.gyldigTilOgMed ? formaterDato(borgerskap.gyldigTilOgMed) : ''}
                </Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell>{!!statsborgerskap && finnLandSomTekst(statsborgerskap, landListe)}</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
