import { Tidsperiode } from './Tidsperiode'
import styled from 'styled-components'
import { numberToProsentString, startdatoOffsetProsent, tidsperiodeProsent } from './tidslinjeUtils'
import { ITidslinjePeriode } from '../../types'

export const TidslinjeRad = ({
  perioder,
  doedsdato,
  seksAarTidligere,
}: {
  perioder: ITidslinjePeriode[]
  doedsdato: string
  seksAarTidligere: string
}) => {
  const startDatoerOffset = perioder.map((periode) => startdatoOffsetProsent(periode.innhold.fraDato, seksAarTidligere))
  const lengdePerioder = perioder.map((periode) =>
    tidsperiodeProsent(periode.innhold.fraDato, periode.innhold.tilDato, doedsdato, seksAarTidligere)
  )
  const justerteStartdatoerOffset = startdatoSammeRadOffsetProsent(startDatoerOffset, lengdePerioder)

  function startdatoSammeRadOffsetProsent(startDatoer: number[], lengdePerioder: number[]): string[] {
    const content = []
    let totalLengde = 0

    for (let i = 0; i < lengdePerioder.length; i++) {
      let offsetStartDato

      if (i === 0) {
        offsetStartDato = startDatoer[i]
        totalLengde = totalLengde + lengdePerioder[i]
      } else {
        offsetStartDato = startDatoer[i] - totalLengde
        totalLengde = lengdePerioder[i] + totalLengde
      }

      content.push(offsetStartDato)
    }
    return content.map((prosent) => numberToProsentString(prosent))
  }

  return (
    <TidsperiodeWrapper>
      {perioder.map((periode, index) => (
        <Tidsperiode
          periode={periode}
          lengde={numberToProsentString(lengdePerioder[index])}
          startOffset={justerteStartdatoerOffset[index]}
          key={index}
        />
      ))}
    </TidsperiodeWrapper>
  )
}

const TidsperiodeWrapper = styled.div`
  display: flex;
`
