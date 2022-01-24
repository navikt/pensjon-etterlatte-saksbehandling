import { IOppholdstillatelse } from './types'
import { PassportIcon } from '../../../shared/icons/passportIcon'
import { Table } from '@navikt/ds-react'
import { TableWrapper } from './styled'
import { useState } from 'react'
import { TextButton } from './TextButton'
import { formatterDato } from '../../../utils/index'

type Props = {
  innhold: IOppholdstillatelse[]
}

export const Oppholdstillatelse: React.FC<Props> = ({ innhold }) => {
  const [visLogg, setVisLogg] = useState(false)

  const getOppholdstillatelser = () => {
    if (!visLogg) {
      const gjeldeneOppholdstillatelser = innhold.filter((item) => !item.oppholdTil)
      return gjeldeneOppholdstillatelser
    } else {
      return innhold
    }
  }

  return (
    <div>
      <TableWrapper>
        <div className="icon">
          <PassportIcon />
        </div>
        <Table className="table">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell className="tableCell">Oppholdstillatelse</Table.HeaderCell>
              <Table.HeaderCell>Periode</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {getOppholdstillatelser().map((element, key) => (
              <Table.Row key={key}>
                <Table.DataCell className="tableCell">{element.type}</Table.DataCell>
                <Table.DataCell>
                  {formatterDato(element.oppholdFra)} - <br />
                  {element.oppholdTil && formatterDato(element.oppholdTil)}
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
        <TextButton isOpen={visLogg} setIsOpen={setVisLogg} antall={innhold.filter((item) => item.oppholdTil).length} />
      </TableWrapper>
    </div>
  )
}
