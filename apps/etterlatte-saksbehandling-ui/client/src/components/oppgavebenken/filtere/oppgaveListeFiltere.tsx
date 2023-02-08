import { IdType, Row } from 'react-table'
import { format } from 'date-fns'
import { IOppgave } from '../typer/oppgavebenken'
import { getContainsSelectFilter } from '../OppgaveListe'

export function globalFilterFunction(rows: Row<IOppgave>[], columnIds: IdType<IOppgave>[], filterValue: string) {
  return rows.filter((row) => {
    return columnIds.some((id) => {
      const rowValue = row.values[id]
      if (id === 'regdato' || id === 'fristdato') {
        return format(rowValue, 'dd.MM.yyyy') === filterValue
      } else if (id === 'enhet' || id === 'oppgaveStatus' || id === 'prioritet') {
        return getContainsSelectFilter(id, rowValue, filterValue)
      }

      return String(rowValue).toLowerCase().includes(String(filterValue).toLowerCase())
    })
  })
}
