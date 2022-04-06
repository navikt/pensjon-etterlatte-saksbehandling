import { Label } from '@navikt/ds-react'
import { DetailWrapper } from '../../styled'
import { format } from 'date-fns'
import { IPersonOpplysningFraPdl } from '../../../types'

export const DoedsfallDato = ({
  avdoedPersonPdl,
  dodsfallMerEnn3AarSiden,
}: {
  avdoedPersonPdl: IPersonOpplysningFraPdl
  dodsfallMerEnn3AarSiden: boolean
}) => {
  return (
    <DetailWrapper>
      <Label size="small" className='headertext'>Dato for dødsfall</Label>
      <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
        {format(new Date(avdoedPersonPdl?.doedsdato), 'dd.MM.yyyy')}
      </span>
    </DetailWrapper>
  )
}
