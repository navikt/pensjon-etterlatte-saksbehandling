import { IStatsborgerskap } from './types'
import { GlobeIcon } from '../../../shared/icons/globeIcon'
import { Table } from '@navikt/ds-react'
import { TableWrapper } from './styled'
import { useState } from 'react'
import { TextButton } from './TextButton'
import { formatterDato } from '../../../utils/index'

type Props = {
  innhold: IStatsborgerskap[]
}

export const Statsborgerskap: React.FC<Props> = ({ innhold }) => {
  const [visLogg, setVisLogg] = useState(false)

  const getStatsborgerskap = () => {
    if (!visLogg) {
      const gjeldeneStatsborgerskap = innhold.filter((item) => !item.gyldigTilOgMed)
      return gjeldeneStatsborgerskap
    } else {
      return innhold
    }
  }

  return (
    <TableWrapper>
      <div className="icon">{<GlobeIcon />}</div>
      <Table className="table">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell className="tableCell">Statsborgerskap</Table.HeaderCell>
            <Table.HeaderCell>Periode</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {getStatsborgerskap().map((element, key) => (
            <Table.Row key={key}>
              <Table.DataCell className="tableCell">{element.land}</Table.DataCell>
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
