import { Label } from '@navikt/ds-react'
import { DetailWrapper } from '../../styled'
import { hentVirkningstidspunkt } from '../../utils'
import { format } from 'date-fns'
import { IPersonOpplysningFraPdl } from '../../../types'

export const Virkningstidspunkt = ({
  avdoedPersonPdl,
  mottattDato,
  dodsfallMerEnn3AarSiden,
}: {
  avdoedPersonPdl: IPersonOpplysningFraPdl
  mottattDato: string
  dodsfallMerEnn3AarSiden: boolean
}) => {
  return (
    <DetailWrapper>
      <Label size="small" className={dodsfallMerEnn3AarSiden ? "" :"headertext"}>
        Første mulig virkningstidspunkt
      </Label>
      <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
        {format(new Date(hentVirkningstidspunkt(avdoedPersonPdl?.doedsdato, mottattDato)), 'dd.MM.yyyy')}
      </span>
    </DetailWrapper>
  )
}
