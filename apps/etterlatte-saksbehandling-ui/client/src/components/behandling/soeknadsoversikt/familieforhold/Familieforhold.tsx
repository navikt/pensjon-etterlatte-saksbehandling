import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { FamilieforholdBarnepensjon } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import { FamilieforholdOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/FamilieforholdOmstillingsstoenad'
import { Box, Heading } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import { Personopplysninger } from '~shared/types/grunnlag'
import styled from 'styled-components'
import { RedigerFamilieforhold } from '~components/behandling/soeknadsoversikt/familieforhold/RedigerFamilieforhold'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand, ILand } from '~shared/api/trygdetid'
import { mapApiResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger | null
  redigerbar: boolean
}

export const Familieforhold = ({ behandling, personopplysninger, redigerbar }: PropsFamilieforhold) => {
  const [landListeResult, landListeFetch] = useApiCall(hentAlleLand)
  useEffect(() => {
    landListeFetch(null)
  }, [])

  return (
    <>
      <Box paddingInline="16" paddingBlock="4">
        <Heading spacing size="medium" level="2" as="div">
          Familieforhold
          {personopplysninger && redigerbar && (
            <RedigerFamilieforhold behandling={behandling} personopplysninger={personopplysninger} />
          )}
        </Heading>
      </Box>

      <FamilieforholdWrapper>
        {behandling.sakType === SakType.BARNEPENSJON ? (
          <FamilieforholdBarnepensjon personopplysninger={personopplysninger} landListeResult={landListeResult} />
        ) : (
          <FamilieforholdOmstillingsstoenad personopplysninger={personopplysninger} landListeResult={landListeResult} />
        )}
      </FamilieforholdWrapper>
    </>
  )
}

export const visLandInfoFraKodeverkEllerDefault = (landListeResult: Result<ILand[]>, statsborgerskap?: string) => {
  return mapApiResult(
    landListeResult,
    <Spinner label="Henter landliste" />,
    () => <>{statsborgerskap ?? 'Ukjent'}</>,
    (landListe) => <>{statsborgerskap ? finnLandSomTekst(statsborgerskap, landListe) : 'Ukjent'}</>
  )
}

export const FamilieforholdWrapper = styled.div`
  padding: 1em 4em;
  display: grid;
  gap: 4rem;
  margin-bottom: 4rem;
`
