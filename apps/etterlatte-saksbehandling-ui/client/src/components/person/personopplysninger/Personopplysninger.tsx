import React, { ReactNode, useEffect } from 'react'
import { LenkeTilAndreSystemer } from '~components/person/personopplysninger/LenkeTilAndreSystemer'
import { Bostedsadresser } from '~components/person/personopplysninger/Bostedsadresser'
import { isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Statsborgerskap } from '~components/person/personopplysninger/Statsborgerskap'
import { Box, Heading, ReadMore, VStack } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'
import { Foreldre } from '~components/person/personopplysninger/Foreldre'
import { AvdoedesBarn } from '~components/person/personopplysninger/AvdoedesBarn'
import { Sivilstatus } from '~components/person/personopplysninger/Sivilstatus'
import { Innflytting } from '~components/person/personopplysninger/Innflytting'
import { hentAlleLand } from '~shared/api/trygdetid'
import { Utflytting } from '~components/person/personopplysninger/Utflytting'
import { Vergemaal } from '~components/person/personopplysninger/Vergemaal'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import styled from 'styled-components'

export const Personopplysninger = ({
  sakResult,
  fnr,
}: {
  sakResult: Result<SakMedBehandlinger>
  fnr: string
}): ReactNode => {
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

  const [landListeResult, landListeFetch] = useApiCall(hentAlleLand)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      familieOpplysningerFetch({ ident: fnr, sakType: sakResult.data.sak.sakType })
    }
  }, [fnr, sakResult])

  useEffect(() => {
    landListeFetch(null)
  }, [])

  return (
    <Box padding="8">
      <VStack gap="4">
        {mapSuccess(sakResult, ({ sak }) => (
          <>
            <PDLInfoReadMore header="Personopplysningene kommer i sanntid fra PDL, hva betyr dette for meg?">
              Personopplysningene som vises på denne siden kommer i sanntid fra PDL. Dette betyr at hvis PDL oppdaterer
              informasjonen for en person, vil denne siden også endre seg til å speile det. Det kan derfor være
              forskjell i informasjonen på denne siden og den som er gitt i en behandling.
            </PDLInfoReadMore>
            <LenkeTilAndreSystemer fnr={fnr} />
            {!!sak ? (
              <>
                {mapResult(familieOpplysningerResult, {
                  pending: <Spinner label="Henter opplysninger" />,
                  error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente opplysninger'}</ApiErrorAlert>,
                  success: ({ soeker, avdoede, gjenlevende }) => (
                    <>
                      <Bostedsadresser bostedsadresse={soeker?.bostedsadresse} />
                      {sak.sakType === SakType.BARNEPENSJON && (
                        <Foreldre
                          avdoed={avdoede}
                          gjenlevende={gjenlevende}
                          foreldreansvar={soeker?.familierelasjon?.ansvarligeForeldre}
                        />
                      )}
                      {sak.sakType === SakType.OMSTILLINGSSTOENAD && (
                        <Sivilstatus sivilstand={soeker?.sivilstand} avdoede={avdoede} />
                      )}
                      <AvdoedesBarn avdoede={avdoede} />
                      {mapSuccess(landListeResult, (landListe) => (
                        <>
                          <Statsborgerskap
                            statsborgerskap={soeker?.statsborgerskap}
                            pdlStatsborgerskap={soeker?.pdlStatsborgerskap}
                            landListe={landListe}
                          />
                          <Innflytting innflytting={soeker?.utland?.innflyttingTilNorge} landListe={landListe} />
                          <Utflytting utflytting={soeker?.utland?.utflyttingFraNorge} landListe={landListe} />
                          <Vergemaal vergemaalEllerFremtidsfullmakt={soeker?.vergemaalEllerFremtidsfullmakt} />
                        </>
                      ))}
                    </>
                  ),
                })}
              </>
            ) : (
              <Heading size="medium">Bruker har ingen sak i Gjenny</Heading>
            )}
          </>
        ))}
      </VStack>
    </Box>
  )
}

const PDLInfoReadMore = styled(ReadMore)`
  max-width: 43.5rem;
`
