import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { HouseIcon } from '@navikt/aksel-icons'
import { IAdresse } from '~shared/types/IAdresse'
import { Heading, Table, Tag } from '@navikt/ds-react'
import { SpaceChildren } from '~shared/styled'
import { formaterStringDato } from '~utils/formattering'

export const Bostedsadresser = ({ bostedsadresse }: { bostedsadresse?: IAdresse[] }): ReactNode => {
  return (
    <Personopplysning heading="Bostedsadresser" icon={<HouseIcon height="2rem" width="2rem" />}>
      {bostedsadresse && bostedsadresse.length >= 0 ? (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader scope="col">Adresse</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Fra og med</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Til og med</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {bostedsadresse.map((adresse: IAdresse, index: number) => (
              <Table.Row key={index}>
                <Table.DataCell>
                  <SpaceChildren direction="row">
                    {`${adresse.adresseLinje1}, ${adresse.postnr} ${!!adresse.poststed ? `, ${adresse.poststed}` : ''}`}
                    {adresse.aktiv && (
                      <Tag variant="success" size="small">
                        Gjeldene
                      </Tag>
                    )}
                  </SpaceChildren>
                </Table.DataCell>
                <Table.DataCell>{formaterStringDato(adresse.gyldigFraOgMed)}</Table.DataCell>
                <Table.DataCell>
                  {!!adresse.gyldigTilOgMed ? formaterStringDato(adresse.gyldigTilOgMed) : ''}
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      ) : (
        <Heading size="medium">Ingen addresser</Heading>
      )}
    </Personopplysning>
  )
}
