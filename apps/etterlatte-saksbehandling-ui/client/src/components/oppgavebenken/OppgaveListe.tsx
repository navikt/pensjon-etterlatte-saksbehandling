/* eslint-disable react/jsx-key */
import React, { useEffect } from 'react'
import 'react-table'
import {
  Column,
  ColumnInstance,
  IdType,
  Row,
  useFilters,
  useGlobalFilter,
  usePagination,
  useSortBy,
  useTable,
} from 'react-table'
import { FilterPar, IOppgave, StatusFilter, statusFilter } from './typer/oppgavebenken'
import { ariaSortMap, FeltSortOrder } from './typer/oppgavefelter'
import { CollapseFilled, ExpandFilled } from '@navikt/ds-icons'
import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { globalFilterFunction, tildeltFilterFunction } from './filtere/oppgaveListeFiltere'
import { useAppSelector } from '../../store/Store'

type Props = {
  columns: ReadonlyArray<Column<IOppgave>>
  data: ReadonlyArray<IOppgave>
  globalFilterValue: string | undefined
  filterPar: Array<FilterPar>
}

const OppgaveListe: React.FC<Props> = ({ columns, data, globalFilterValue, filterPar }) => {
  const saksbehandlerNavn = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler.navn)

  const filterTypes = React.useMemo(
    () => ({
      globalFilter: globalFilterFunction,
      tildeltFilter: (rows: Row<IOppgave>[], columnIds: IdType<IOppgave>[], filterValue: string) =>
        tildeltFilterFunction(rows, columnIds, filterValue, saksbehandlerNavn),
    }),
    [globalFilterValue, filterPar]
  )

  const {
    getTableProps,
    getTableBodyProps,
    headerGroups,
    page,
    prepareRow,
    setGlobalFilter,
    setAllFilters,
    canPreviousPage,
    canNextPage,
    pageCount,
    gotoPage,
    nextPage,
    previousPage,
    setPageSize,
    filteredRows,
    state: { pageIndex, pageSize },
  } = useTable(
    {
      columns,
      data,
      filterTypes,
      globalFilter: filterTypes.globalFilter,
      initialState: {
        sortBy: [
          {
            id: 'regdato',
            desc: true,
          },
        ],
      },
    },
    useFilters,
    useGlobalFilter,
    useSortBy,
    usePagination
  )

  useEffect(() => {
    setAllFilters(filterPar)
  }, [filterPar])

  useEffect(() => {
    setGlobalFilter(globalFilterValue)
  }, [globalFilterValue])

  return (
    <Styles>
      <Heading size={'medium'} spacing>
        Oppgaveliste
      </Heading>
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
          {page.map((row) => {
            prepareRow(row)
            return (
              <tr {...row.getRowProps()}>
                {row.cells.map((cell) => {
                  return <td {...cell.getCellProps()}>{cell.render('Cell')}</td>
                })}
              </tr>
            )
          })}
        </tbody>
      </table>
      <div className={'pagination'}>
        <button onClick={() => gotoPage(0)} disabled={!canPreviousPage}>
          {'<<'}
        </button>
        <button onClick={() => previousPage()} disabled={!canPreviousPage}>
          {'<'}
        </button>
        <button onClick={() => nextPage()} disabled={!canNextPage}>
          {'>'}
        </button>
        <button onClick={() => gotoPage(pageCount - 1)} disabled={!canNextPage}>
          {'>>'}
        </button>
        <span>
          Viser {pageIndex * pageSize + 1} - {pageIndex * pageSize + page.length} av {filteredRows.length} oppgaver
          (totalt {data.length} oppgaver)
        </span>
        <select
          value={pageSize}
          onChange={(e) => {
            setPageSize(Number(e.target.value))
          }}
        >
          {[10, 20, 30, 40, 50].map((pageSize) => (
            <option key={pageSize} value={pageSize}>
              Vis {pageSize}
            </option>
          ))}
        </select>
      </div>
    </Styles>
  )
}

export function getContainsSelectFilter(id: string, rowValue: string, filterValue: string) {
  let navn = ''
  if (id === 'status') {
    navn = statusFilter[rowValue as StatusFilter]?.navn.toLowerCase()
  }

  return navn.includes(String(filterValue).toLowerCase())
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

const Styles = styled.div`
  table {
    border-spacing: 0;

    th {
      padding: 1rem;
      border-bottom: 1px solid black;
      text-align: left;
    }

    tr,
    td {
      margin: 0;
      padding: 1rem;
      border-bottom: 1px solid grey;
      min-width: 170px;
    }
  }

  .pagination {
    padding-top: 1rem;

    button {
      padding: 0 0.5rem 0 0.5rem;
      margin: 0 1px 0 1px;
    }

    span {
      margin: 0 1rem 0 1rem;
    }
  }
`

export default OppgaveListe
