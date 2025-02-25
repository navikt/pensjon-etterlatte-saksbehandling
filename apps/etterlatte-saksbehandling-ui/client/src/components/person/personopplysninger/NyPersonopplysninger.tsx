import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { hentAlleLand } from '~shared/api/behandling'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { VStack } from '@navikt/ds-react'
import { BostedsadresserExpansionCard } from '~components/person/personopplysninger/opplysninger/BostedsadresserExpansionCard'
import { VergemaalExpansionCard } from '~components/person/personopplysninger/opplysninger/VergemaalExpansionCard'

interface Props {
  sakResult: Result<SakMedBehandlinger>
  fnr: string
}

export const NyPersonopplysninger = ({ sakResult, fnr }: Props) => {
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

  const [alleLandResult, alleLandFetch] = useApiCall(hentAlleLand)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      familieOpplysningerFetch({ ident: fnr, sakType: sakResult.data.sak.sakType })
    }
  }, [fnr, sakResult])

  useEffect(() => {
    alleLandFetch(null)
  }, [])

  return mapResult(sakResult, {
    pending: <Spinner label="Henter sak..." />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente sak'}</ApiErrorAlert>,
    success: ({ sak }) =>
      mapResult(familieOpplysningerResult, {
        pending: <Spinner label="Henter familieopplysninger..." />,
        error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente familieopplysninger'}</ApiErrorAlert>,
        success: ({ soeker, avdoede, gjenlevende }) => (
          <VStack padding="8" gap="4">
            <BostedsadresserExpansionCard bostedsadresser={soeker?.bostedsadresse} />
            <VergemaalExpansionCard vergemaal={soeker?.vergemaalEllerFremtidsfullmakt} />
          </VStack>
        ),
      }),
  })
}
