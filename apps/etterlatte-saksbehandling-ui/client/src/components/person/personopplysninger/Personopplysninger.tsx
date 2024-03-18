import React, { ReactNode, useEffect } from 'react'
import { Container, SpaceChildren } from '~shared/styled'
import { LenkeTilAndreSystemer } from '~components/person/personopplysninger/LenkeTilAndreSystemer'
import { Bostedsadresser } from '~components/person/personopplysninger/Bostedsadresser'
import { isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonopplysningerForBehandling } from '~shared/api/grunnlag'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Statsborgerskap } from '~components/person/personopplysninger/Statsborgerskap'
import { Alert, Heading } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'
import { Foreldre } from '~components/person/personopplysninger/Foreldre'
import { AvdoedesBarn } from '~components/person/personopplysninger/AvdoedesBarn'
import { Sivilstatus } from '~components/person/personopplysninger/Sivilstatus'
import { Innflytting } from '~components/person/personopplysninger/Innflytting'
import { hentAlleLand } from '~shared/api/trygdetid'
import { Utflytting } from '~components/person/personopplysninger/Utflytting'
import { Vergemaal } from '~components/person/personopplysninger/Vergemaal'

export const Personopplysninger = ({
  sakStatus,
  fnr,
}: {
  sakStatus: Result<SakMedBehandlinger>
  fnr: string
}): ReactNode => {
  const [personopplysningerResult, hentPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)
  const [landListeResult, hentLandListe] = useApiCall(hentAlleLand)

  useEffect(() => {
    if (isSuccess(sakStatus)) {
      hentPersonopplysninger({
        behandlingId: sakStatus.data.behandlinger[0].id,
        sakType: sakStatus.data.behandlinger[0].sakType,
      })
    }
  }, [fnr, sakStatus])

  useEffect(() => {
    hentLandListe(null)
  }, [])

  return (
    <Container>
      <SpaceChildren>
        {mapSuccess(sakStatus, (sak) => (
          <>
            <Alert variant="warning">
              Denne informasjonen baserer seg på når en behandling var opprettet på brukeren, vi jobber med å få
              informasjonen til å oppdatere seg i sanntid.
            </Alert>
            <LenkeTilAndreSystemer fnr={fnr} />
            {!!sak.behandlinger?.length ? (
              <>
                {mapResult(personopplysningerResult, {
                  pending: <Spinner visible={true} label="Henter personopplysninger" />,
                  error: (error) => (
                    <ApiErrorAlert>{error.detail || 'Kunne ikke hente personopplysninger'}</ApiErrorAlert>
                  ),
                  success: (personopplysninger) => (
                    <>
                      <Bostedsadresser bostedsadresse={personopplysninger.soeker?.opplysning.bostedsadresse} />
                      {sak.sak.sakType === SakType.BARNEPENSJON && (
                        <Foreldre
                          avdoed={personopplysninger.avdoede}
                          gjenlevende={personopplysninger.gjenlevende}
                          foreldreansvar={personopplysninger.soeker?.opplysning.familieRelasjon?.ansvarligeForeldre}
                        />
                      )}
                      {sak.sak.sakType === SakType.OMSTILLINGSSTOENAD && (
                        <Sivilstatus
                          sivilstand={personopplysninger.soeker?.opplysning.sivilstand}
                          avdoede={personopplysninger.avdoede}
                        />
                      )}
                      <AvdoedesBarn avdoede={personopplysninger.avdoede} />
                      {mapSuccess(landListeResult, (landListe) => (
                        <>
                          <Statsborgerskap
                            statsborgerskap={personopplysninger.soeker?.opplysning.statsborgerskap}
                            pdlStatsborgerskap={personopplysninger.soeker?.opplysning.pdlStatsborgerskap}
                            bosattLand={personopplysninger.soeker?.opplysning.bostedsadresse?.at(0)?.land}
                            landListe={landListe}
                          />
                          <Innflytting
                            innflytting={personopplysninger.soeker?.opplysning.utland?.innflyttingTilNorge}
                            landListe={landListe}
                          />
                          <Utflytting
                            utflytting={personopplysninger.soeker?.opplysning.utland?.utflyttingFraNorge}
                            landListe={landListe}
                          />
                          <Vergemaal
                            vergemaalEllerFremtidsfullmakt={
                              personopplysninger.soeker?.opplysning.vergemaalEllerFremtidsfullmakt
                            }
                          />
                        </>
                      ))}
                    </>
                  ),
                })}
              </>
            ) : (
              <Heading size="medium">Bruker har ingen behandling i Gjenny</Heading>
            )}
          </>
        ))}
      </SpaceChildren>
    </Container>
  )
}
