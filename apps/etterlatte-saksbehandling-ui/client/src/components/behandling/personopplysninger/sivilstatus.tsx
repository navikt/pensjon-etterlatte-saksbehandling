import { useState } from 'react'
import { ISivilstand } from './types'
import { HeartIcon } from '../../../shared/icons/heartIcon'
import { Table } from '@navikt/ds-react'
import { TableWrapper } from './styled'
import { TextButton } from './TextButton'
import { formatterDato } from '../../../utils/index'
type Props = {
  innhold: ISivilstand[]
}

export const Sivilstatus: React.FC<Props> = ({ innhold }) => {
  const [visLogg, setVisLogg] = useState(false)

  const getSivilstatuser = () => {
    if (!visLogg) {
      const gjeldeneSivilstatus = innhold.filter((item) => !item.gyldigTilOgMed)
      return gjeldeneSivilstatus
    } else {
      return innhold
    }
  }
  return (
    <div>
      <TableWrapper>
        <div className="icon">
          <HeartIcon />
        </div>
        <Table className="table">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell className="tableCell">Sivilstatus</Table.HeaderCell>
              <Table.HeaderCell>Periode</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {getSivilstatuser().map((element, key) => (
              <Table.Row key={key}>
                <Table.DataCell className="tableCell">{element.sivilstandType}</Table.DataCell>
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
    </div>
  )
}
