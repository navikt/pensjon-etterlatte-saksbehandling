import { mapResult } from '~shared/api/apiUtils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { ILand } from '~utils/kodeverk'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersongalleriSamsvar } from '~shared/api/grunnlag'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { PersongalleriProblem, PersongalleriSamsvar } from '~shared/types/grunnlag'
import { SakType } from '~shared/types/sak'
import { SoekerErEndretAlert } from '~components/behandling/soeknadsoversikt/familieforhold/persongalleriSamsvar/SoekerErEndretAlert'
import { VStack } from '@navikt/ds-react'
import { HarAvvikMotPdlAlert } from '~components/behandling/soeknadsoversikt/familieforhold/persongalleriSamsvar/HarAvvikMotPdlAlert'

const sjekkOmPersongalleriProblemErRelevantForSakType = (problem: PersongalleriProblem, sakType: SakType) => {
  switch (problem) {
    case 'ENDRET_SOEKER_FNR':
      return true
    case 'EKSTRA_AVDOED':
    case 'MANGLER_AVDOED':
      return true
    case 'EKSTRA_GJENLEVENDE':
    case 'MANGLER_GJENLEVENDE':
      return sakType === SakType.BARNEPENSJON
    case 'EKSTRA_SOESKEN':
    case 'HAR_PERSONER_UTEN_IDENTER':
    // Håndteres separat fra person problem
    case 'MANGLER_SOESKEN':
      return false
  }
}

interface Props {
  behandling: IDetaljertBehandling
  alleLand: ILand[]
}

export const PersongalleriSamsvarAlerts = ({ behandling, alleLand }: Props) => {
  const [persongalleriSamsvarResult, persongalleriSamsvarFetch] = useApiCall(hentPersongalleriSamsvar)

  const soekerErEndret = (persongalleriSamsvar: PersongalleriSamsvar) =>
    persongalleriSamsvar.problemer.includes('ENDRET_SOEKER_FNR')
  const harPersonerUtenIdenter = (persongalleriSamsvar: PersongalleriSamsvar) =>
    persongalleriSamsvar.problemer.includes('HAR_PERSONER_UTEN_IDENTER')

  const harAvvikMotPdl = (persongalleriSamsvar: PersongalleriSamsvar) =>
    persongalleriSamsvar.problemer.filter((problem) =>
      sjekkOmPersongalleriProblemErRelevantForSakType(problem, behandling.sakType)
    ).length > 0

  useEffect(() => {
    persongalleriSamsvarFetch({ behandlingId: behandling.id })
  }, [])

  return mapResult(persongalleriSamsvarResult, {
    pending: <Spinner label="Sjekker samsvar for persongalleri..." />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke sjekke samsvar for persongalleri'}</ApiErrorAlert>,
    success: (persongalleriSamsvar) =>
      !!persongalleriSamsvar.problemer?.length && (
        <VStack gap="4">
          {soekerErEndret(persongalleriSamsvar) ? (
            <SoekerErEndretAlert persongalleriSamsvar={persongalleriSamsvar} />
          ) : (
            <>
              {harAvvikMotPdl(persongalleriSamsvar) && (
                <HarAvvikMotPdlAlert persongalleriSamsvar={persongalleriSamsvar} sakType={behandling.sakType} />
              )}
            </>
          )}
        </VStack>
      ),
  })
}
