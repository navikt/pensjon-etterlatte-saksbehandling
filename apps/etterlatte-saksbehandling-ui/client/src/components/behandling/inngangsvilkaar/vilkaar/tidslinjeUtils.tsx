import moment from 'moment'

export function aarIProsent(femAarTidligere: string, doedsdato: string): string[][] {
  const antallDagerMellomDatoer = moment.duration(moment(doedsdato).diff(moment(femAarTidligere))).asDays()
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
  doedsdato: string,
  femAarTidligere: string
): string {
  const maxDate = moment(doedsdato, 'DD.MM.YYYY').diff(moment(tilDato, 'DD.MM.YYYY')) < 0 ? doedsdato : tilDato
  const miDate =
    moment(femAarTidligere, 'DD.MM.YYYY').diff(moment(fraDato, 'DD.MM.YYYY')) > 0 ? femAarTidligere : fraDato

  const periode = moment.duration(moment(maxDate, 'DD.MM.YYYY').diff(moment(miDate, 'DD.MM.YYYY'))).asDays()

  if (periode > moment.duration(5, 'years').asDays()) {
    return '100%'
  } else {
    return prosentAvFemAar(periode)
  }
}

export function startdatoOffsetProsent(fraDato: string, femAarTidligere: string) {
  if (moment(fraDato, 'DD.MM.YYYY').diff(moment(femAarTidligere, 'DD.MM.YYYY')) < 0) {
    return '0%'
  } else {
    return prosentAvFemAar(
      moment.duration(moment(fraDato, 'DD.MM.YYYY').diff(moment(femAarTidligere, 'DD.MM.YYYY'))).asDays()
    )
  }
}

function prosentAvFemAar(antallDager: number): string {
  return ((antallDager / moment.duration(5, 'years').asDays()) * 100).toString() + '%'
}
