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
