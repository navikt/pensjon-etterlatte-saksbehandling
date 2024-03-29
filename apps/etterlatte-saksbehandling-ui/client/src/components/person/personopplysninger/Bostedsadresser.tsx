import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { HouseIcon } from '@navikt/aksel-icons'
import { IAdresse } from '~shared/types/IAdresse'
import { Heading, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'

export const Bostedsadresser = ({ bostedsadresse }: { bostedsadresse?: IAdresse[] }): ReactNode => {
  return (
    <Personopplysning heading="Bostedsadresser" icon={<HouseIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Adresse</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fra og med</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Til og med</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!bostedsadresse?.length ? (
            <>
              {bostedsadresse.map((adresse: IAdresse, index: number) => (
                <Table.Row key={index}>
                  <BostedsadresseDataCell bostedsadresse={bostedsadresse} index={index} visAktiv />
                  <Table.DataCell>{formaterStringDato(adresse.gyldigFraOgMed)}</Table.DataCell>
                  <Table.DataCell>
                    {!!adresse.gyldigTilOgMed ? formaterStringDato(adresse.gyldigTilOgMed) : ''}
                  </Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={3}>
                <Heading size="medium">Ingen bostedsadresser</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
