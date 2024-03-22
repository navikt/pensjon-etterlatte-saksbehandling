import React from 'react'
import { IAdresse } from '~shared/types/IAdresse'
import { SpaceChildren } from '~shared/styled'
import { Table, Tag } from '@navikt/ds-react'

export const BostedsadresseDataCell = ({
  bostedsadresse,
  index,
  visAktiv,
}: {
  bostedsadresse?: IAdresse[]
  index: number
  visAktiv?: boolean
}) => {
  return (
    <>
      {!!bostedsadresse && !!bostedsadresse[index] ? (
        <Table.DataCell>
          <SpaceChildren direction="row">
            {`${!!bostedsadresse[index].adresseLinje1 ? bostedsadresse[index].adresseLinje1 : '-'}, ${!!bostedsadresse[index].postnr ? bostedsadresse[index].postnr : ''} ${!!bostedsadresse[index].poststed ? bostedsadresse[index].poststed : ''}`}
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
