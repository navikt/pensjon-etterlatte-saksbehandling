import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { KildePdl } from '~shared/types/kilde'
import { IPdlPerson } from '~shared/types/Person'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useEffect } from 'react'
import { formaterKildePdl } from '~components/behandling/soeknadsoversikt/utils'

interface Props {
  behandlingId: string
  sakId: number
}

export const Verger = ({ sakId, behandlingId }: Props) => {
  const [soeker, getSoekerFraGrunnlag] = useApiCall(getGrunnlagsAvOpplysningstype)
  useEffect(() => {
    getSoekerFraGrunnlag({
      sakId: sakId,
      behandlingId: behandlingId,
      opplysningstype: 'SOEKER_PDL_V1',
    })
  }, [sakId, behandlingId])

  function successContents(soekerOpplysning: Grunnlagsopplysning<IPdlPerson, KildePdl>) {
    const vergeList = soekerOpplysning.opplysning.vergemaalEllerFremtidsfullmakt || []
    if (vergeList?.length == 0) {
      return <Info label="Verge" tekst="Ingen verge registrert" undertekst={formaterKildePdl(soekerOpplysning.kilde)} />
    }

    return (
      <>
        {vergeList.map((it, index) => (
          <Info
            label="Verge"
            tekst={it.vergeEllerFullmektig.motpartsPersonident}
            undertekst={formaterKildePdl(soekerOpplysning.kilde)}
            key={index}
          />
        ))}
      </>
    )
  }

  return (
    <InfoWrapper>
      {isSuccess(soeker) && successContents(soeker.data)}
      {isFailure(soeker) && <ApiErrorAlert>Kunne ikke hente info om verger</ApiErrorAlert>}
    </InfoWrapper>
  )
}
