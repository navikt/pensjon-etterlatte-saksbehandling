import React, { useEffect } from 'react'
import 'react-table'
import { useTable, Column, useFilters, useGlobalFilter } from 'react-table'
import { IOppgave } from '../../typer/oppgavebenken'

type Props = {
  columns: ReadonlyArray<Column<IOppgave>>
  data: ReadonlyArray<IOppgave>
  globalFilter: string
}

const OppgaveListe: React.FC<Props> = ({ columns, data, globalFilter }) => {
  const { getTableProps, getTableBodyProps, headerGroups, rows, prepareRow, setGlobalFilter } = useTable(
    { columns, data },
    useFilters,
    useGlobalFilter
  )

  useEffect(() => {
    setGlobalFilter(globalFilter)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [globalFilter])

  return (
    <>
      <table {...getTableProps()}>
        <thead>
          {headerGroups.map((headerGroup) => (
            <tr {...headerGroup.getHeaderGroupProps()}>
              {headerGroup.headers.map((column) => (
                <th
                  {...column.getHeaderProps()}
                  style={{
                    color: 'black',
                  }}
                >
                  {column.render('Header')}
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

export default OppgaveListe
