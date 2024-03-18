import React, { ReactNode, useEffect, useState } from 'react'
import { Container, SpaceChildren } from '~shared/styled'
import { LenkeTilAndreSystemer } from '~components/person/personopplysninger/LenkeTilAndreSystemer'
import { Bostedsadresser } from '~components/person/personopplysninger/Bostedsadresser'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
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
import { hentAlleLand, ILand } from '~shared/api/trygdetid'
import { Utflytting } from '~components/person/personopplysninger/Utflytting'
import { Vergemaal } from '~components/person/personopplysninger/Vergemaal'

export const Personopplysninger = ({
  sakStatus,
  fnr,
}: {
  sakStatus: Result<SakMedBehandlinger>
  fnr: string
}): ReactNode => {
  const [landListe, setLandListe] = useState<ILand[]>([])

  const [personopplysningerResult, hentPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)
  const [, hentLandListe] = useApiCall(hentAlleLand)

  const erSaktype = (sakStatus: Result<SakMedBehandlinger>, sakType: SakType) => {
    return isSuccess(sakStatus) && sakStatus.data.sak.sakType === sakType
  }

  useEffect(() => {
    if (isSuccess(sakStatus)) {
      hentPersonopplysninger({
        behandlingId: sakStatus.data.behandlinger[0].id,
        sakType: sakStatus.data.behandlinger[0].sakType,
      })
    }
  }, [sakStatus])

  useEffect(() => {
    hentLandListe(null, setLandListe)
  }, [])

  return (
    <Container>
      <SpaceChildren>
        {!!sakStatus ? (
          <>
            <Alert variant="warning">Denne informasjonen baserer seg på når en behandling var opprettet på brukeren, vi jobber med å få informasjonen til å oppdatere seg i sanntid.</Alert>
            <LenkeTilAndreSystemer fnr={fnr} />
            {mapResult(personopplysningerResult, {
              pending: <Spinner visible={true} label="Henter personopplysninger" />,
              error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente personopplysninger'}</ApiErrorAlert>,
              success: (personopplysninger) => (
                <>
                  <Bostedsadresser bostedsadresse={personopplysninger.soeker?.opplysning.bostedsadresse} />
                  {erSaktype(sakStatus, SakType.BARNEPENSJON) && (
                    <Foreldre
                      avdoed={personopplysninger.avdoede}
                      gjenlevende={personopplysninger.gjenlevende}
                      foreldreansvar={personopplysninger.soeker?.opplysning.familieRelasjon?.ansvarligeForeldre}
                    />
                  )}
                  {erSaktype(sakStatus, SakType.OMSTILLINGSSTOENAD) && (
                    <Sivilstatus
                      sivilstand={personopplysninger.soeker?.opplysning.sivilstand}
                      avdoede={personopplysninger.avdoede}
                    />
                  )}
                  <AvdoedesBarn avdoede={personopplysninger.avdoede} />
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
              ),
            })}
          </>
        ) : (
          <Heading size="medium">{`Person med fnr: ${fnr} har ingen sak i Gjenny`}</Heading>
        )}
      </SpaceChildren>
    </Container>
  )
}
