import React, { ReactNode, useEffect } from 'react'
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
import { Heading } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'
import { Foreldre } from '~components/person/personopplysninger/Foreldre'
import { AvdoedesBarn } from '~components/person/personopplysninger/AvdoedesBarn'

export const Personopplysninger = ({
  sakStatus,
  fnr,
}: {
  sakStatus: Result<SakMedBehandlinger>
  fnr: string
}): ReactNode => {
  const [personopplysningerResult, hentPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)

  useEffect(() => {
    if (isSuccess(sakStatus)) {
      hentPersonopplysninger({
        behandlingId: sakStatus.data.behandlinger[0].id,
        sakType: sakStatus.data.behandlinger[0].sakType,
      })
    }
  }, [sakStatus])

  const erSaktype = (sakStatus: Result<SakMedBehandlinger>, sakType: SakType) => {
    return isSuccess(sakStatus) && sakStatus.data.sak.sakType === sakType
  }

  return (
    <Container>
      <SpaceChildren>
        {!!sakStatus ? (
          <>
            <LenkeTilAndreSystemer fnr={fnr} />
            {mapResult(personopplysningerResult, {
              pending: <Spinner visible={true} label="Henter personopplysninger" />,
              error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente personopplysninger'}</ApiErrorAlert>,
              success: (personopplysninger) => (
                <>
                  <Bostedsadresser bostedsadresse={personopplysninger.soeker?.opplysning.bostedsadresse} />
                  {/*TODO gå igjennom og sjekk hva som kan gjenbrukes på tvers av BP og OMS*/}
                  {erSaktype(sakStatus, SakType.BARNEPENSJON) && (
                    <>
                      <Foreldre
                        avdoed={personopplysninger.avdoede}
                        gjenlevende={personopplysninger.gjenlevende}
                        foreldreansvar={personopplysninger.soeker?.opplysning.familieRelasjon?.ansvarligeForeldre}
                      />
                      <AvdoedesBarn
                        avdoede={personopplysninger.avdoede}
                        foreldreansvar={personopplysninger.soeker?.opplysning.familieRelasjon?.ansvarligeForeldre}
                      />
                    </>
                  )}
                  <Statsborgerskap
                    bostedsLand={personopplysninger.soeker?.opplysning.bostedsadresse?.at(0)?.land}
                    pdlStatsborgerskap={personopplysninger.soeker?.opplysning.pdlStatsborgerskap}
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
