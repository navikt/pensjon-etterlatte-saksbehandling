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

export const Personopplysninger = ({ sakStatus }: { sakStatus: Result<SakMedBehandlinger> }): ReactNode => {
  const [personopplysningerResult, hentPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)

  useEffect(() => {
    if (isSuccess(sakStatus)) {
      hentPersonopplysninger({
        behandlingId: sakStatus.data.behandlinger[0].id,
        sakType: sakStatus.data.behandlinger[0].sakType,
      })
    }
  }, [sakStatus])

  return (
    <Container>
      <SpaceChildren>
        <LenkeTilAndreSystemer />
        {mapResult(personopplysningerResult, {
          pending: <Spinner visible={true} label="Henter personopplysninger" />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente personopplysninger'}</ApiErrorAlert>,
          success: (personopplysninger) => <Bostedsadresser />,
        })}
      </SpaceChildren>
    </Container>
  )
}
