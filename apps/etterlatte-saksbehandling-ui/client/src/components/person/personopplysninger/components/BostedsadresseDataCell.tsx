import React from 'react'
import { SpaceChildren } from '~shared/styled'
import { Table, Tag } from '@navikt/ds-react'
import { Bostedsadresse } from '~shared/api/pdltjenester'

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
          <SpaceChildren direction="row">
            {`${!!bostedsadresse[index].adresse ? bostedsadresse[index].adresse : '-'}, ${!!bostedsadresse[index].postnr ? bostedsadresse[index].postnr : ''}`}
            {bostedsadresse[index].aktiv && visAktiv && (
              <Tag variant="success" size="small">
                Gjeldende
              </Tag>
            )}
          </SpaceChildren>
        </Table.DataCell>
      ) : (
        <Table.DataCell>Ingen bostedsadresse tilgjengelig</Table.DataCell>
      )}
    </>
  )
}
