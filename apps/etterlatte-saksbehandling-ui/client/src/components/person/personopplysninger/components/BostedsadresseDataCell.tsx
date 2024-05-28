import React from 'react'
import { HStack, Table, Tag } from '@navikt/ds-react'
import { Bostedsadresse } from '~shared/types/familieOpplysninger'

export const BostedsadresseDataCell = ({
  bostedsadresse,
  index,
  visAktiv,
}: {
  bostedsadresse?: Bostedsadresse[]
  index: number
  visAktiv?: boolean
}) => {
  return (
    <>
      {!!bostedsadresse && !!bostedsadresse[index] ? (
        <Table.DataCell>
          <HStack gap="4">
            <>
              {!!bostedsadresse[index].adresse ? bostedsadresse[index].adresse : '-'},
              {!!bostedsadresse[index].postnr ? bostedsadresse[index].postnr : ''}
            </>
            {bostedsadresse[index].aktiv && visAktiv && (
              <Tag variant="success" size="small">
                Gjeldende
              </Tag>
            )}
          </HStack>
        </Table.DataCell>
      ) : (
        <Table.DataCell>Ingen bostedsadresse tilgjengelig</Table.DataCell>
      )}
    </>
  )
}
