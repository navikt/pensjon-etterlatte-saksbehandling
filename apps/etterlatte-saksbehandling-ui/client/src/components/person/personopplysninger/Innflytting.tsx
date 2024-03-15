import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { AirplaneIcon } from '@navikt/aksel-icons'
import { Heading, Table } from '@navikt/ds-react'
import { ILand } from '~shared/api/trygdetid'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'

interface InnflyttingDTO {
  fraflyttingsland?: string
  dato?: string
}

export const Innflytting = ({
  innflytting,
  landListe,
}: {
  innflytting?: InnflyttingDTO[]
  landListe: ILand[]
}): ReactNode => {
  return (
    <Personopplysning heading="Innflytting" icon={<AirplaneIcon height="2rem" width="2rem" />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Innflyttet fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Innflyttet år</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {innflytting && innflytting.length > 0 ? (
            <>
              {innflytting.map((flytting: InnflyttingDTO, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    {!!flytting.fraflyttingsland && finnLandSomTekst(flytting.fraflyttingsland, landListe)}
                  </Table.DataCell>
                  <Table.DataCell>{!!flytting.dato && new Date(flytting.dato).getFullYear()}</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell>
                <Heading size="small">Ingen innflytting</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
