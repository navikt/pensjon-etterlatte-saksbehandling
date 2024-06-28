import { isBefore } from 'date-fns'
import { formaterDato } from '~utils/formatering/dato'
import classnames from 'classnames'

export const FristWrapper = ({ dato }: { dato?: string }) => {
  const fristHarPassert = !!dato && isBefore(new Date(dato), new Date())

  return (
    <span className={classnames({ 'navds-error-message': fristHarPassert })}>
      {dato ? formaterDato(dato) : 'Ingen frist'}
    </span>
  )
}
