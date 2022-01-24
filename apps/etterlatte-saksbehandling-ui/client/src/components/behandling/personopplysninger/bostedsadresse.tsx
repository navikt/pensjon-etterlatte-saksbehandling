import { IBostedsadresse } from './types'
import { HomeIcon } from '../../../shared/icons/homeIcon'
import { Table } from '@navikt/ds-react'
import { TableWrapper } from './styled'
import { TextButton } from './TextButton'
import { useState } from 'react'
import { formatterDato } from '../../../utils/index'

type Props = {
  innhold: IBostedsadresse[]
}

export const Bostedsadresse: React.FC<Props> = ({ innhold }) => {
  const [visLogg, setVisLogg] = useState(false)

  const getBostedsadresser = () => {
    if (!visLogg) {
      const gjeldeneBostedsadresser = innhold.filter((item) => !item.gyldigTilOgMed)
      return gjeldeneBostedsadresser
    } else {
      return innhold
    }
  }

  return (
    <TableWrapper>
      <div className="icon">
        <HomeIcon />
      </div>
      <Table className="table">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell className="tableCell">Bostedsadresse</Table.HeaderCell>
            <Table.HeaderCell>Periode</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {getBostedsadresser().map((element, key) => (
            <Table.Row key={key}>
              <Table.DataCell className="tableCell">
                {element.adressenavn} <br /> {element.postnummer}
              </Table.DataCell>
              <Table.DataCell>
                {formatterDato(element.gyldigFraOgMed)} - <br />
                {element.gyldigTilOgMed && formatterDato(element.gyldigTilOgMed)}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <TextButton
        isOpen={visLogg}
        setIsOpen={setVisLogg}
        antall={innhold.filter((item) => item.gyldigTilOgMed).length}
      />
    </TableWrapper>
  )
}
