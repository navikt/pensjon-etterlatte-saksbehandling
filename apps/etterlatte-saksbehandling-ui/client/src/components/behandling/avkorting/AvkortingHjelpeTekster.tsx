import { NOK } from '~utils/formatering/formatering'
import { HelpText } from '@navikt/ds-react'
import React, { useState } from 'react'
import { trackClick } from '~utils/amplitude'

export const ForventetInntektHeaderHjelpeTekst = () => {
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
    <HelpText title="Hva innebærer forventet inntekt totalt" onClick={sendAmplitudeKlikkEvent}>
      Forventet inntekt totalt er registrert inntekt Norge pluss inntekt utland minus eventuelt fratrekk for inn-år.
      Beløpet vil automatisk avrundes ned til nærmeste tusen når avkorting beregnes.
    </HelpText>
  )
}

export const InnvilgaMaanederHeaderHjelpeTekst = () => {
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
    <HelpText title="Hva betyr innvilgede måneder" onClick={sendAmplitudeKlikkEvent}>
      Her vises antall måneder med innvilget stønad i gjeldende inntektsår. Registrert forventet inntekt, med eventuelt
      fratrekk for inntekt opptjent før/etter innvilgelse, blir fordelt på de innvilgede månedene. Antallet vil ikke
      endres selv om man tar en inntektsendring i løpet av året.
    </HelpText>
  )
}

export const ForventetInntektHjelpeTekst = ({
  aarsinntekt,
  fratrekkInnAar,
  forventetInntekt,
}: {
  aarsinntekt: number
  fratrekkInnAar: number
  forventetInntekt: number
}) => {
  return (
    <HelpText title="Se hva forventet inntekt består av">
      Forventet inntekt beregnes utfra forventet årsinntekt med fratrekk for måneder før innvilgelse.
      <br />
      Forventet inntekt Norge = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
      {` ${NOK(aarsinntekt)} - ${NOK(fratrekkInnAar)} = ${NOK(forventetInntekt)}`}).
    </HelpText>
  )
}

export const ForventetInntektUtlandHjelpeTekst = ({
  inntektUtland,
  fratrekkUtland,
  forventetInntektUtland,
}: {
  inntektUtland: number
  fratrekkUtland: number
  forventetInntektUtland: number
}) => {
  return (
    <HelpText title="Se hva forventet inntekt består av">
      Forventet inntekt utland beregnes utfra inntekt utland med fratrekk for måneder før innvilgelse.
      <br />
      Forventet inntekt utland = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
      {` ${NOK(inntektUtland)} - ${NOK(fratrekkUtland)} = ${NOK(forventetInntektUtland)}`}).
    </HelpText>
  )
}
