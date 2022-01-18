import { IdType, Row } from 'react-table'
import { IOppgave, SaksbehandlerFilter } from '../typer/oppgavebenken'
import { getContainsSelectFilter } from '../OppgaveListe'
import moment from 'moment'

export function globalFilterFunction(rows: Row<IOppgave>[], columnIds: IdType<IOppgave>[], filterValue: string) {
  return rows.filter((row) => {
    return columnIds.some((id) => {
      const rowValue = row.values[id]
      if (id === 'regdato' || id === 'fristdato') {
        return moment(rowValue).format('DD.MM.YYYY').toString() === filterValue
      } else if (id === 'enhet' || id === 'status' || id === 'prioritet') {
        return getContainsSelectFilter(id, rowValue, filterValue)
      }

      return String(rowValue).toLowerCase().includes(String(filterValue).toLowerCase())
    })
  })
}

export function tildeltFilterFunction(
  rows: Row<IOppgave>[],
  columnIds: IdType<IOppgave>[],
  filterValue: string,
  saksbehandlerNavn: string
) {
  return rows.filter((row) => {
    return columnIds.some((id) => {
      const mappedRowValue: SaksbehandlerFilter = mapRowValueToSaksbehandlerFilterValue(
        row.values[id],
        saksbehandlerNavn
      )
      return mappedRowValue === filterValue
    })
  })
}

export function mapRowValueToSaksbehandlerFilterValue(rowValue: string, saksbehandlerNavn: string) {
  if (rowValue === saksbehandlerNavn) {
    return SaksbehandlerFilter.INNLOGGET
  } else if (rowValue === '') {
    return SaksbehandlerFilter.UFORDELTE
  } else if (rowValue) {
    return SaksbehandlerFilter.FORDELTE
  }

  return SaksbehandlerFilter.ALLE
}
