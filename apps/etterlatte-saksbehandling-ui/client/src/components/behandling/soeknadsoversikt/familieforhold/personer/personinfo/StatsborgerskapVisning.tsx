import { Statsborgerskap } from '~shared/types/Person'
import { Box, Label } from '@navikt/ds-react'
import { UstiletListe } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import { ListeItemMedSpacingIMellom } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/UtvandringInnvandring'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'
import { Result } from '~shared/api/apiUtils'
import { ILand } from '~shared/api/trygdetid'
import { visLandInfoFraKodeverkEllerDefault } from '~components/behandling/soeknadsoversikt/familieforhold/Familieforhold'

export function StatsborgerskapVisning(props: {
  statsborgerskap?: string
  pdlStatsborgerskap?: Statsborgerskap[]
  landListeResult: Result<ILand[]>
}) {
  const { statsborgerskap, pdlStatsborgerskap, landListeResult } = props

  if (!pdlStatsborgerskap) {
    return (
      <Box paddingBlock="2 0">
        <Label as="p">Statsborgerskap</Label>
        <span>{visLandInfoFraKodeverkEllerDefault(landListeResult, statsborgerskap)}</span>
      </Box>
    )
  }

  return (
    <Box paddingBlock="2 0">
      <Label as="p">Statsborgerskap</Label>
      <UstiletListe>
        {pdlStatsborgerskap!!.map((statsborgerskap, index) => (
          <ListeItemMedSpacingIMellom key={index}>
            <UstiletListe>
              <li>Land: {statsborgerskap.land}</li>
              <li>
                Gyldig fra og med: {formaterKanskjeStringDatoMedFallback('ukjent', statsborgerskap.gyldigFraOgMed)}
              </li>
              <li>
                Gyldig til og med: {formaterKanskjeStringDatoMedFallback('ukjent', statsborgerskap.gyldigTilOgMed)}
              </li>
            </UstiletListe>
          </ListeItemMedSpacingIMellom>
        ))}
      </UstiletListe>
    </Box>
  )
}
