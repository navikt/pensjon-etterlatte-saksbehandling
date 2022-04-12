import { differenceInDays, add, startOfYear, format, differenceInMilliseconds } from "date-fns"


export function aarIProsent(femAarTidligere: string, doedsdato: string): string[][] {
  // const antallDagerMellomDatoer = moment.duration(doedsdato.diff(femAarTidligere)).asDays()
  const antallDagerMellomDatoer = differenceInDays(new Date(doedsdato), new Date(femAarTidligere))
  const antallAar = [1, 2, 3, 4]

  const aarstallMellomDatoerNavn = antallAar.map((aar) =>
    format(startOfYear(add(new Date(femAarTidligere), {years: aar})), "yyyy")
  )

  const startOfAar = antallAar.map((aar) => startOfYear(add(new Date(femAarTidligere), {years: aar})))
  const startOfAarProsent = startOfAar.map(
    (aar) =>
      (
        (differenceInDays(aar, new Date(femAarTidligere)) / antallDagerMellomDatoer) *
        100
      ).toString() + '%'
  )
  return aarstallMellomDatoerNavn.map((aar, index) => [aar, startOfAarProsent[index]])
}

export function tidsperiodeProsent(
  fraDato: string,
  tilDato: string,
  doedsdato: string,
  femAarTidligere: string
): string {
  const maxDate = differenceInMilliseconds(new Date(doedsdato), new Date(tilDato)) < 0 ? doedsdato : tilDato
  const minDate = differenceInMilliseconds(new Date(femAarTidligere), new Date(fraDato)) > 0 ? femAarTidligere : fraDato

  const periode = differenceInDays(new Date(maxDate), new Date(minDate))

  if (periode > (5*365)) { //antall dager på fem år?
    return '100%'
  } else {
    return prosentAvFemAar(periode)
  }
}

export function startdatoOffsetProsent(fraDato: string, femAarTidligere: string) {
  if (differenceInMilliseconds(new Date(fraDato), new Date(femAarTidligere)) < 0) {
    return '0%'
  } else {
    return prosentAvFemAar(differenceInDays(new Date(fraDato), new Date(femAarTidligere)))
  }
}

function prosentAvFemAar(antallDager: number): string {
  return (antallDager / (5*365) * 100).toString() + '%'
}
