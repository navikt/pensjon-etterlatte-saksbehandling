import React from 'react'
import { HStack, Table, Tag } from '@navikt/ds-react'
import { Bostedsadresse } from '~shared/types/familieOpplysninger'

export const BostedsadresseDataCell = ({
  bostedsadresser,
  index,
  visAktiv,
}: {
  bostedsadresser?: Bostedsadresse[]
  index: number
  visAktiv?: boolean
}) => {
  return (
    <>
      {!!bostedsadresser && !!bostedsadresser[index] ? (
        <Table.DataCell>
          <HStack gap="space-4">
            <>
              {!!bostedsadresser[index].adresse ? bostedsadresser[index].adresse : '-'},{' '}
              {!!bostedsadresser[index].postnr ? bostedsadresser[index].postnr : ''}
            </>
            {bostedsadresser[index].aktiv && visAktiv && (
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
