import { differenceInDays, add, startOfYear, format, differenceInMilliseconds } from 'date-fns'

export function aarIProsent(seksAarTidligere: string, doedsdato: string): string[][] {
  const antallDagerMellomDatoer = differenceInDays(new Date(doedsdato), new Date(seksAarTidligere))
  const antallAar = [1, 2, 3, 4, 5, 6]

  const aarstallMellomDatoerNavn = antallAar.map((aar) =>
    format(startOfYear(add(new Date(seksAarTidligere), { years: aar })), 'yyyy')
  )

  const startOfAar = antallAar.map((aar) => startOfYear(add(new Date(seksAarTidligere), { years: aar })))
  const startOfAarProsent = startOfAar.map(
    (aar) => ((differenceInDays(aar, new Date(seksAarTidligere)) / antallDagerMellomDatoer) * 100).toString() + '%'
  )
  return aarstallMellomDatoerNavn.map((aar, index) => [aar, startOfAarProsent[index]])
}

export function tidsperiodeProsent(
  fraDato: string,
  tilDato: string | undefined,
  doedsdato: string,
  femAarTidligere: string
): number {
  const tilDatoNy = tilDato == undefined ? doedsdato : tilDato
  const maxDate = differenceInMilliseconds(new Date(doedsdato), new Date(tilDatoNy)) < 0 ? doedsdato : tilDatoNy
  const minDate = differenceInMilliseconds(new Date(femAarTidligere), new Date(fraDato)) > 0 ? femAarTidligere : fraDato

  const periode = differenceInDays(new Date(maxDate), new Date(minDate))

  if (periode > 6 * 365) {
    return 100
  } else {
    return prosentAvSeksAar(periode)
  }
}

export function startdatoOffsetProsent(fraDato: string, seksAarTidligere: string): number {
  if (differenceInMilliseconds(new Date(fraDato), new Date(seksAarTidligere)) < 0) {
    return 0
  } else {
    return prosentAvSeksAar(differenceInDays(new Date(fraDato), new Date(seksAarTidligere)))
  }
}

function prosentAvSeksAar(antallDager: number): number {
  return (antallDager / (6 * 365)) * 100
}

export function numberToProsentString(number: number): string {
  return number.toString() + '%'
}
