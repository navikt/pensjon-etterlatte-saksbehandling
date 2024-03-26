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
        {mapSuccess(sakResult, (sak) => (
          <>
            <LenkeTilAndreSystemer fnr={fnr} />
            {!!sak.sak ? (
              <>
                {mapResult(personopplysningerResult, {
                  pending: <Spinner visible={true} label="Henter personopplysninger" />,
                  error: (error) => (
                    <ApiErrorAlert>{error.detail || 'Kunne ikke hente personopplysninger'}</ApiErrorAlert>
                  ),
                  success: (personopplysninger) => (
                    <>
                      <Bostedsadresser bostedsadresse={personopplysninger.soeker?.bostedsadresse} />
                      {sak.sak.sakType === SakType.BARNEPENSJON && (
                        <Foreldre
                          avdoed={personopplysninger.avdoede}
                          gjenlevende={personopplysninger.gjenlevende}
                          foreldreansvar={personopplysninger.soeker?.familierelasjon?.ansvarligeForeldre}
                        />
                      )}
                      {sak.sak.sakType === SakType.OMSTILLINGSSTOENAD && (
                        <Sivilstatus
                          sivilstand={personopplysninger.soeker?.sivilstand}
                          avdoede={personopplysninger.avdoede}
                        />
                      )}
                      <AvdoedesBarn avdoede={personopplysninger.avdoede} />
                      {mapSuccess(landListeResult, (landListe) => (
                        <>
                          <Statsborgerskap
                            statsborgerskap={personopplysninger.soeker?.statsborgerskap}
                            pdlStatsborgerskap={personopplysninger.soeker?.pdlStatsborgerskap}
                            landListe={landListe}
                          />
                          <Innflytting
                            innflytting={personopplysninger.soeker?.utland?.innflyttingTilNorge}
                            landListe={landListe}
                          />
                          <Utflytting
                            utflytting={personopplysninger.soeker?.utland?.utflyttingFraNorge}
                            landListe={landListe}
                          />
                          <Vergemaal
                            vergemaalEllerFremtidsfullmakt={personopplysninger.soeker?.vergemaalEllerFremtidsfullmakt}
                          />
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
