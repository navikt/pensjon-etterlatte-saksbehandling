import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'

interface Props {
  size?: 'small' | 'medium' | 'large'
  width?: string // TODO: Bytte om til Aksel design token som type når v7 er lansert
  colHeaders: Array<string>
  dataRows: ReactNode | Array<ReactNode>
}

export const Tabell = ({ size, width, colHeaders, dataRows }: Props) => {
  return (
    <Table size={size || 'medium'} zebraStripes style={{ width: width || '100%' }}>
      <Table.Header>
        <Table.Row>
          {colHeaders.map((col, index) => (
            <Table.HeaderCell key={index} scope="col">
              {col}
            </Table.HeaderCell>
          ))}
        </Table.Row>
      </Table.Header>
      <Table.Body>{dataRows}</Table.Body>
    </Table>
  )
}
