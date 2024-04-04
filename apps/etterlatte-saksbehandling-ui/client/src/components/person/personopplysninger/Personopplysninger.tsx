import React, { ReactNode, useEffect } from 'react'
import { Container, SpaceChildren } from '~shared/styled'
import { LenkeTilAndreSystemer } from '~components/person/personopplysninger/LenkeTilAndreSystemer'
import { Bostedsadresser } from '~components/person/personopplysninger/Bostedsadresser'
import { isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Statsborgerskap } from '~components/person/personopplysninger/Statsborgerskap'
import { Heading } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'
import { Foreldre } from '~components/person/personopplysninger/Foreldre'
import { AvdoedesBarn } from '~components/person/personopplysninger/AvdoedesBarn'
import { Sivilstatus } from '~components/person/personopplysninger/Sivilstatus'
import { Innflytting } from '~components/person/personopplysninger/Innflytting'
import { hentAlleLand } from '~shared/api/trygdetid'
import { Utflytting } from '~components/person/personopplysninger/Utflytting'
import { Vergemaal } from '~components/person/personopplysninger/Vergemaal'
import { hentPersonopplysninger } from '~shared/api/pdltjenester'
import { PDLInfoAlert } from '~components/person/personopplysninger/components/PDLInfoAlert'

export const Personopplysninger = ({
  sakResult,
  fnr,
}: {
  sakResult: Result<SakMedBehandlinger>
  fnr: string
}): ReactNode => {
  const [personopplysningerResult, personopplysningerFetch] = useApiCall(hentPersonopplysninger)

  const [landListeResult, landListeFetch] = useApiCall(hentAlleLand)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      personopplysningerFetch({ ident: fnr, sakType: sakResult.data.sak.sakType })
    }
  }, [fnr, sakResult])

  useEffect(() => {
    landListeFetch(null)
  }, [])

  return (
    <Container>
      <SpaceChildren>
        {mapSuccess(sakResult, ({ sak }) => (
          <>
            <PDLInfoAlert />
            <LenkeTilAndreSystemer fnr={fnr} />
            {!!sak ? (
              <>
                {mapResult(personopplysningerResult, {
                  pending: <Spinner visible={true} label="Henter personopplysninger" />,
                  error: (error) => (
                    <ApiErrorAlert>{error.detail || 'Kunne ikke hente personopplysninger'}</ApiErrorAlert>
                  ),
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
      </SpaceChildren>
    </Container>
  )
}
