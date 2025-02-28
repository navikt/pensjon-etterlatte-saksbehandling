import { KildeSaksbehandler } from '~shared/types/kilde'
import { Detail, VStack } from '@navikt/ds-react'
import { formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import React from 'react'

export const VurderingKilde = ({ kilde }: { kilde: KildeSaksbehandler }) => {
  return (
    <VStack>
      <Detail>Manuelt av {kilde.ident}</Detail>
      <Detail>Sist endret {kilde.tidspunkt ? formaterDatoMedTidspunkt(new Date(kilde.tidspunkt)) : '-'}</Detail>
    </VStack>
  )
}
