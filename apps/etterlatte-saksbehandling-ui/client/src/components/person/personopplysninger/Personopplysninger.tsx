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
import { SivilstandExpansionCard } from '~components/person/personopplysninger/opplysninger/SivilstandExpansionCard'
import { AvdoedesBarnExpansionCard } from '~components/person/personopplysninger/opplysninger/AvdoedesBarnExpansionCard'
import { StatsborgerskapExpansionCard } from '~components/person/personopplysninger/opplysninger/StatsborgerskapExpansionCard'
import { InnflyttingExpansionCard } from '~components/person/personopplysninger/opplysninger/InnflyttingExpansionCard'
import { UtflyttingExpansionCard } from '~components/person/personopplysninger/opplysninger/UtflyttingExpansionCard'
import { BrukersOpplysningerIAndreSystemerExpansionCard } from '~components/person/personopplysninger/opplysninger/BrukersOpplysningerIAndreSystemerExpansionCard'

interface Props {
  sakResult: Result<SakMedBehandlinger>
  fnr: string
}

export const Personopplysninger = ({ sakResult, fnr }: Props) => {
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

  return mapResult(familieOpplysningerResult, {
    pending: <Spinner label="Henter familieopplysninger..." />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente familieopplysninger'}</ApiErrorAlert>,
    success: ({ soeker, avdoede }) => (
      <VStack padding="space-8" gap="space-8">
        <BrukersOpplysningerIAndreSystemerExpansionCard fnr={fnr} />
        <HStack gap="space-4" wrap={false}>
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
        <AvdoedesBarnExpansionCard avdoede={avdoede} />
        <HStack gap="space-4" wrap={false}>
          <Box width="100%">
            <SivilstandExpansionCard sivilstand={soeker?.sivilstand} avdoede={avdoede} />
          </Box>
          <Box width="100%">
            <SivilstandExpansionCard
              sivilstand={avdoede?.flatMap((avdoed) => avdoed.sivilstand ?? [])}
              avdoede={avdoede}
              erAvdoedesSivilstand
            />
          </Box>
        </HStack>
        {mapResult(alleLandResult, {
          pending: <Spinner label="Henter alle land..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente alle land'}</ApiErrorAlert>,
          success: (alleLand) => (
            <>
              <HStack gap="space-4" wrap={false}>
                <Box width="100%">
                  <StatsborgerskapExpansionCard
                    statsborgerskap={soeker?.statsborgerskap ? [soeker.statsborgerskap] : []}
                    pdlStatsborgerskap={soeker?.pdlStatsborgerskap}
                    alleLand={alleLand}
                  />
                </Box>
                <Box width="100%">
                  <StatsborgerskapExpansionCard
                    statsborgerskap={avdoede?.flatMap((avdoed) => avdoed.statsborgerskap ?? [])}
                    pdlStatsborgerskap={avdoede?.flatMap((avdoed) => avdoed.pdlStatsborgerskap ?? [])}
                    alleLand={alleLand}
                    erAvdoedesStatsborgerskap
                  />
                </Box>
              </HStack>
              <HStack gap="space-4" wrap={false}>
                <Box width="100%">
                  <InnflyttingExpansionCard innflytting={soeker?.utland?.innflyttingTilNorge} alleLand={alleLand} />
                </Box>
                <Box width="100%">
                  <InnflyttingExpansionCard
                    innflytting={avdoede?.flatMap((avdoed) => avdoed.utland?.innflyttingTilNorge ?? [])}
                    alleLand={alleLand}
                    erAvdoedesInnflytting
                  />
                </Box>
              </HStack>
              <HStack gap="space-4" wrap={false}>
                <Box width="100%">
                  <UtflyttingExpansionCard utflytting={soeker?.utland?.utflyttingFraNorge} alleLand={alleLand} />
                </Box>
                <Box width="100%">
                  <UtflyttingExpansionCard
                    utflytting={avdoede?.flatMap((avdoed) => avdoed.utland?.utflyttingFraNorge ?? [])}
                    alleLand={alleLand}
                    erAvdoedesUtflytting
                  />
                </Box>
              </HStack>
            </>
          ),
        })}
      </VStack>
    ),
  })
}
