import { Statsborgerskap } from '~shared/types/Person'
import { PersonDetailWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Label } from '@navikt/ds-react'
import { UstiletListe } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import { ListeItemMedSpacingIMellom } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/UtvandringInnvandring'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formattering'

export function Statsborgerskap(props: { statsborgerskap?: string; pdlStatsborgerskap?: Statsborgerskap[] }) {
  const { statsborgerskap, pdlStatsborgerskap } = props

  if (!statsborgerskap && !pdlStatsborgerskap) {
    return (
      <PersonDetailWrapper adresse={false}>
        <Label as="p">Statsborgerskap</Label>
        <span>Ukjent</span>
      </PersonDetailWrapper>
    )
  }

  if (!!statsborgerskap) {
    return (
      <PersonDetailWrapper adresse={false}>
        <Label as="p">Statsborgerskap</Label>
        <span>{statsborgerskap}</span>
      </PersonDetailWrapper>
    )
  }

  return (
    <PersonDetailWrapper adresse={false}>
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
    </PersonDetailWrapper>
  )
}
