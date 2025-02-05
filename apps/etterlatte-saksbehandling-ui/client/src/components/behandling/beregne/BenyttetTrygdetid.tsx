export const BenyttetTrygdetid = ({
  trygdetid,
  beregningsMetode,
  samletNorskTrygdetid,
  samletTeoretiskTrygdetid,
}: {
  trygdetid: number
  beregningsMetode: string | undefined
  samletNorskTrygdetid: number | undefined
  samletTeoretiskTrygdetid: number | undefined
}) => {
  let benyttetTrygdetid = trygdetid

  if (beregningsMetode === 'NASJONAL' && samletNorskTrygdetid) {
    benyttetTrygdetid = samletNorskTrygdetid
  }

  if (beregningsMetode === 'PRORATA' && samletTeoretiskTrygdetid) {
    benyttetTrygdetid = samletTeoretiskTrygdetid
  }

  return <>{benyttetTrygdetid} år</>
}
