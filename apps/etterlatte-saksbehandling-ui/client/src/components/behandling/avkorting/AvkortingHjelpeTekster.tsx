import { NOK } from '~utils/formatering/formatering'
import { HelpText } from '@navikt/ds-react'
import React, { useState } from 'react'
import { trackClick } from '~utils/amplitude'

export const ForventetInntektHjelpeTekst = ({
  aarsinntekt,
  fratrekkInnAar,
  forventetInntekt,
}: {
  aarsinntekt: number
  fratrekkInnAar: number
  forventetInntekt: number
}) => {
  const [aapen, setAapen] = useState<boolean>(false)

  const sendAmplitudeKlikkEvent = () => {
    if (!aapen) {
      setAapen(true)
      trackClick('avkorting forventet inntekt hjelpetekst klikk')
    } else {
      setAapen(false)
    }
  }

  return (
    <HelpText title="Se hva forventet inntekt består av" onClick={sendAmplitudeKlikkEvent}>
      Forventet inntekt beregnes utfra forventet årsinntekt med fratrekk for måneder før innvilgelse.
      <br />
      Forventet inntekt Norge = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
      {` ${NOK(aarsinntekt)} - ${NOK(fratrekkInnAar)} = ${NOK(forventetInntekt)}`}).
    </HelpText>
  )
}
