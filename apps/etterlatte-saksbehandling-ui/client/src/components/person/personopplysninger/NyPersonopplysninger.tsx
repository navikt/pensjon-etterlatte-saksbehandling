import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { hentAlleLand } from '~shared/api/behandling'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Box, HStack, VStack } from '@navikt/ds-react'
import { BostedsadresserExpansionCard } from '~components/person/personopplysninger/opplysninger/BostedsadresserExpansionCard'
import { VergemaalExpansionCard } from '~components/person/personopplysninger/opplysninger/VergemaalExpansionCard'
import { SakType } from '~shared/types/sak'
import { SivilstandExpansionCard } from '~components/person/personopplysninger/opplysninger/SivilstandExpansionCard'
import { AvdoedesBarnExpansionCard } from '~components/person/personopplysninger/opplysninger/AvdoedesBarnExpansionCard'

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
            <HStack gap="4" wrap={false}>
              <Box width="100%">
                <BostedsadresserExpansionCard bostedsadresser={soeker?.bostedsadresse} />
              </Box>
              <Box width="100%">
                <BostedsadresserExpansionCard
                  bostedsadresser={avdoede?.flatMap((avdoed) => avdoed.bostedsadresse ?? [])}
                  erAvdoedesAddresser
                />
              </Box>
            </HStack>
            <VergemaalExpansionCard vergemaal={soeker?.vergemaalEllerFremtidsfullmakt} />
            {sak.sakType === SakType.OMSTILLINGSSTOENAD && (
              <SivilstandExpansionCard sivilstand={soeker?.sivilstand} avdoede={avdoede} />
            )}
            <AvdoedesBarnExpansionCard avdoede={avdoede} />
          </VStack>
        ),
      }),
  })
}
