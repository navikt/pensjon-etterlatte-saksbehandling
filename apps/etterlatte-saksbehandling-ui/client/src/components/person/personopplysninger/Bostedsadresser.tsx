import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { HouseIcon } from '@navikt/aksel-icons'
import { Heading, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { Bostedsadresse } from '~shared/types/familieOpplysninger'
import { compareDesc } from 'date-fns'

export const Bostedsadresser = ({ bostedsadresse }: { bostedsadresse?: Bostedsadresse[] }): ReactNode => {
  const sortedBostedsadresse = bostedsadresse?.sort((a: Bostedsadresse, b: Bostedsadresse) => {
    if (a.gyldigFraOgMed && b.gyldigFraOgMed) {
      return compareDesc(new Date(a.gyldigFraOgMed), new Date(b.gyldigFraOgMed))
    }
    return 0
  })

  return (
    <Personopplysning heading="Bostedsadresser" icon={<HouseIcon aria-hidden />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Adresse</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fra og med</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Til og med</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!sortedBostedsadresse?.length ? (
            <>
              {sortedBostedsadresse.map((adresse: Bostedsadresse, index: number) => (
                <Table.Row key={index}>
                  <BostedsadresseDataCell bostedsadresse={bostedsadresse} index={index} visAktiv />
                  <Table.DataCell>
                    {!!adresse.gyldigFraOgMed ? formaterDato(adresse.gyldigFraOgMed) : ''}
                  </Table.DataCell>
                  <Table.DataCell>
                    {!!adresse.gyldigTilOgMed ? formaterDato(adresse.gyldigTilOgMed) : ''}
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
