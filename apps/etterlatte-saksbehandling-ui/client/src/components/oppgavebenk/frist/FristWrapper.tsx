import { isBefore } from 'date-fns'
import { formaterStringDato } from '~utils/formattering'
import classnames from 'classnames'

export const FristWrapper = ({ dato }: { dato?: string }) => {
  const fristHarPassert = !!dato && isBefore(new Date(dato), new Date())

  return (
    <span className={classnames({ 'navds-error-message': fristHarPassert })}>
      {dato ? formaterStringDato(dato) : 'Ingen frist'}
    </span>
  )
}
