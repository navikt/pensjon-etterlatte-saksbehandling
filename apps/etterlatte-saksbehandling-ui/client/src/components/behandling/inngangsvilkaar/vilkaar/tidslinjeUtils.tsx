import moment, { Moment } from 'moment'

export function aarIProsent(femAarTidligere: Moment, doedsdato: Moment): string[][] {
  const antallDagerMellomDatoer = moment.duration(doedsdato.diff(femAarTidligere)).asDays()
  const antallAar = [1, 2, 3, 4]

  const aarstallMellomDatoerNavn = antallAar.map((aar) =>
    moment(femAarTidligere).add(aar, 'year').startOf('year').format('YYYY').toString()
  )

  const startOfAar = antallAar.map((aar) => moment(femAarTidligere).add(aar, 'year').startOf('year'))
  const startOfAarProsent = startOfAar.map(
    (aar) =>
      (
        (moment.duration(aar.diff(moment(femAarTidligere, 'DD.MM.YYYY'))).asDays() / antallDagerMellomDatoer) *
        100
      ).toString() + '%'
  )
  return aarstallMellomDatoerNavn.map((aar, index) => [aar, startOfAarProsent[index]])
}

export function tidsperiodeProsent(
  fraDato: string,
  tilDato: string,
  doedsdato: Moment,
  femAarTidligere: Moment
): string {
  const maxDate = moment(doedsdato).diff(moment(tilDato)) < 0 ? doedsdato : tilDato
  const minDate = moment(femAarTidligere).diff(moment(fraDato)) > 0 ? femAarTidligere : fraDato

  const periode = moment.duration(moment(maxDate).diff(moment(minDate))).asDays()

  if (periode > moment.duration(5, 'years').asDays()) {
    return '100%'
  } else {
    return prosentAvFemAar(periode)
  }
}

export function startdatoOffsetProsent(fraDato: string, femAarTidligere: Moment) {
  if (moment(fraDato).diff(moment(femAarTidligere)) < 0) {
    return '0%'
  } else {
    return prosentAvFemAar(moment.duration(moment(fraDato).diff(femAarTidligere)).asDays())
  }
}

function prosentAvFemAar(antallDager: number): string {
  return ((antallDager / moment.duration(5, 'years').asDays()) * 100).toString() + '%'
}
