import styled from 'styled-components'
import { Utland } from '~shared/types/Person'
import { PersonDetailWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Label } from '@navikt/ds-react'
import { UstiletListe } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formattering'

export const ListeItemMedSpacingIMellom = styled.li`
  :not(:first-child) {
    margin-top: 0.5rem;
  }
`

export function Utlandsopphold(props: { utland: Utland }) {
  const { utland } = props
  const utflyttinger = utland.utflyttingFraNorge ?? []
  const innflyttinger = utland.innflyttingTilNorge ?? []

  return (
    <>
      <PersonDetailWrapper $adresse={false}>
        <Label as="p">Utvandring</Label>
        {utflyttinger.length > 0 ? (
          <UstiletListe>
            {utflyttinger.map((utflytting, index) => {
              return (
                <ListeItemMedSpacingIMellom key={index}>
                  <UstiletListe>
                    <li>Tilflyttingsland: {utflytting.tilflyttingsland ?? 'ukjent'}</li>
                    <li>Dato: {formaterKanskjeStringDatoMedFallback('ukjent', utflytting.dato)}</li>
                  </UstiletListe>
                </ListeItemMedSpacingIMellom>
              )
            })}
          </UstiletListe>
        ) : (
          <span>Ingen</span>
        )}
      </PersonDetailWrapper>
      <PersonDetailWrapper $adresse={false}>
        <Label as="p">Innvandring</Label>
        {innflyttinger.length > 0 ? (
          <UstiletListe>
            {innflyttinger.map((innflytting, index) => {
              return (
                <li key={index}>
                  <UstiletListe>
                    <li>Fraflyttingsland: {innflytting.fraflyttingsland ?? 'ukjent'}</li>
                    <li>Dato: {formaterKanskjeStringDatoMedFallback('ukjent', innflytting.dato)}</li>
                  </UstiletListe>
                </li>
              )
            })}
          </UstiletListe>
        ) : (
          <span>Ingen</span>
        )}
      </PersonDetailWrapper>
    </>
  )
}
