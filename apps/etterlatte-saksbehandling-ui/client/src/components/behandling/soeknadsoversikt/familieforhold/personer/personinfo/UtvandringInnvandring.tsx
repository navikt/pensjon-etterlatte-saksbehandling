import styled from 'styled-components'
import { Utland } from '~shared/types/Person'
import { Box, Label } from '@navikt/ds-react'
import { UstiletListe } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'
import { visLandInfoFraKodeverkEllerDefault } from '~components/behandling/soeknadsoversikt/familieforhold/Familieforhold'
import { Result } from '~shared/api/apiUtils'
import { ILand } from '~utils/kodeverk'

export const ListeItemMedSpacingIMellom = styled.li`
  :not(:first-child) {
    margin-top: 0.5rem;
  }
`

export function Utlandsopphold({ utland, landListeResult }: { utland: Utland; landListeResult: Result<ILand[]> }) {
  const utflyttinger = utland.utflyttingFraNorge ?? []
  const innflyttinger = utland.innflyttingTilNorge ?? []

  return (
    <>
      <Box paddingBlock="2 0">
        <Label as="p">Utvandring</Label>
        {utflyttinger.length > 0 ? (
          <UstiletListe>
            {utflyttinger.map((utflytting, index) => {
              return (
                <li key={index}>
                  <UstiletListe>
                    <li>
                      Tilflyttingsland:{' '}
                      {visLandInfoFraKodeverkEllerDefault(landListeResult, utflytting.tilflyttingsland)}
                    </li>
                    <li>Dato: {formaterKanskjeStringDatoMedFallback('ukjent', utflytting.dato)}</li>
                  </UstiletListe>
                </li>
              )
            })}
          </UstiletListe>
        ) : (
          <span>Ingen</span>
        )}
      </Box>
      <Box paddingBlock="2 0">
        <Label as="p">Innvandring</Label>
        {innflyttinger.length > 0 ? (
          <UstiletListe>
            {innflyttinger.map((innflytting, index) => {
              return (
                <li key={index}>
                  <UstiletListe>
                    <li>
                      Fraflyttingsland:{' '}
                      {visLandInfoFraKodeverkEllerDefault(landListeResult, innflytting.fraflyttingsland)}
                    </li>
                    <li>Dato: {formaterKanskjeStringDatoMedFallback('ukjent', innflytting.dato)}</li>
                  </UstiletListe>
                </li>
              )
            })}
          </UstiletListe>
        ) : (
          <span>Ingen</span>
        )}
      </Box>
    </>
  )
}
