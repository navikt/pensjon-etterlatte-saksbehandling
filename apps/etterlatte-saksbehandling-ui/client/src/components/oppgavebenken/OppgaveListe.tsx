import React from "react";
import "react-table";
import {useTable, Column} from "react-table";
import {IOppgave} from "../../typer/oppgavebenken";


const OppgaveListe = () => {
    const data: ReadonlyArray<IOppgave>  = React.useMemo(
            () => [
                {
                    id: '1',
                    beskrivelse: 'test 1',
                    status: "Ferdig"
                },
                {
                    id: '2',
                    beskrivelse: 'test 2',
                    status: "Ikke ferdig"
                },
                {
                    id: '3',
                    beskrivelse: 'test 3',
                    status: "Under behandling"
                },
            ],
            []
    )

    const columns: ReadonlyArray<Column<IOppgave>> = React.useMemo(
            () => [
                {
                    Header: 'ID',
                    accessor: 'id',
                },
                {
                    Header: 'Beskrivelse',
                    accessor: 'beskrivelse',
                },
                {
                    Header: 'Status',
                    accessor: 'status',
                },

            ],
            []
    )

    const {
        getTableProps,
        getTableBodyProps,
        headerGroups,
        rows,
        prepareRow,
    } = useTable({ columns, data })

    return (
            <table {...getTableProps()} >
                <thead>
                {headerGroups.map(headerGroup => (
                        <tr {...headerGroup.getHeaderGroupProps()}>
                            {headerGroup.headers.map(column => (
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
                {rows.map(row => {
                    prepareRow(row)
                    return (
                            <tr {...row.getRowProps()}>
                                {row.cells.map(cell => {
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
    )
}


export default OppgaveListe;