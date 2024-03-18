import React, { ReactNode } from 'react'
import { ILand } from '~shared/api/trygdetid'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { AirplaneIcon } from '@navikt/aksel-icons'
import { Heading, Table } from '@navikt/ds-react'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'

interface UtflyttingDTO {
  tilflyttingsland?: string
  dato?: string
}

export const Utflytting = ({
  utflytting,
  landListe,
}: {
  utflytting?: UtflyttingDTO[]
  landListe: ILand[]
}): ReactNode => {
  return (
    <Personopplysning heading="Utflytting" icon={<AirplaneIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Utflyttet fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Utflyttet år</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!utflytting?.length ? (
            <>
              {utflytting.map((flytting: UtflyttingDTO, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    {!!flytting.tilflyttingsland && finnLandSomTekst(flytting.tilflyttingsland, landListe)}
                  </Table.DataCell>
                  <Table.DataCell>{!!flytting.dato && new Date(flytting.dato).getFullYear()}</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell>
                <Heading size="small">Ingen utflyttninger</Heading>
              </Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
