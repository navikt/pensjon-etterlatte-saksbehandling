import React, { useEffect } from 'react'
import 'react-table'
import { useTable, Column, useFilters, useGlobalFilter, useSortBy, ColumnInstance } from 'react-table'
import { FilterPar, IOppgave } from '../../typer/oppgavebenken'
import { ariaSortMap, FeltSortOrder } from './oppgavefelter'
import { CollapseFilled, ExpandFilled } from '@navikt/ds-icons'

type Props = {
  columns: ReadonlyArray<Column<IOppgave>>
  data: ReadonlyArray<IOppgave>
  globalFilter: string | undefined
  filterPar: Array<FilterPar>
}

const OppgaveListe: React.FC<Props> = ({ columns, data, globalFilter, filterPar }) => {
  const { getTableProps, getTableBodyProps, headerGroups, rows, prepareRow, setGlobalFilter, setAllFilters } = useTable(
    {
      columns,
      data,
      initialState: {
        sortBy: [
          {
            id: 'id',
            desc: false,
          },
        ],
      },
    },
    useFilters,
    useGlobalFilter,
    useSortBy
  )

  useEffect(() => {
    setAllFilters(filterPar)
  }, [filterPar, setAllFilters])

  useEffect(() => {
    setGlobalFilter(globalFilter)
  }, [globalFilter, setGlobalFilter])

  return (
    <>
      <table {...getTableProps()}>
        <thead>
          {headerGroups.map((headerGroup) => (
            <tr {...headerGroup.getHeaderGroupProps()}>
              {headerGroup.headers.map((column) => (
                <th
                  role="columnheader"
                  aria-sort={getAriaSort(column)}
                  {...column.getHeaderProps(column.getSortByToggleProps())}
                >
                  {column.render('Header')}
                  <span>
                    {column.isSorted ? (
                      column.isSortedDesc ? (
                        <ExpandFilled color={'var(--navds-button-color-secondary-text)'} />
                      ) : (
                        <CollapseFilled color={'var(--navds-button-color-secondary-text)'} />
                      )
                    ) : (
                      ''
                    )}
                  </span>
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody {...getTableBodyProps()}>
          {rows.map((row) => {
            prepareRow(row)
            return (
              <tr {...row.getRowProps()}>
                {row.cells.map((cell) => {
                  return (
                    <td
                      {...cell.getCellProps()}
                      style={{
                        padding: '10px',
                      }}
                    >
                      {cell.render('Cell')}
                    </td>
                  )
                })}
              </tr>
            )
          })}
        </tbody>
      </table>
    </>
  )
}

export const getAriaSort = (column: ColumnInstance<IOppgave>): 'none' | 'descending' | 'ascending' | undefined => {
  if (column.isSortedDesc === true) {
    return ariaSortMap.get(FeltSortOrder.DESCENDANT)
  }
  if (column.isSortedDesc === false) {
    return ariaSortMap.get(FeltSortOrder.ASCENDANT)
  }
  return ariaSortMap.get(FeltSortOrder.NONE)
}

export default OppgaveListe
