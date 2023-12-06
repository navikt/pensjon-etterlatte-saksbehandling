import { InfoWrapper } from '../styled'
import { Info } from '../Info'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { KildePdl } from '~shared/types/kilde'
import { IPdlPerson } from '~shared/types/Person'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { useEffect } from 'react'
import { formaterKildePdl } from '~components/behandling/soeknadsoversikt/utils'

import { isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { formaterFnr } from '~utils/formattering'

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

    const omfangMap = new Map([
      ['personligeOgOekonomiskeInteresser', 'Personlige og økonomiske interesser'],
      ['utlendingssakerPersonligeOgOekonomiskeInteresser', 'Personlige og økonomiske interesser (utlendingssaker)'],
      ['oekonomiskeInteresser', 'Økonomiske interesser'],
      ['personligeInteresser', 'Personlige interesser'],
    ])

    return (
      <>
        {vergeList.map((it, index) => (
          <Info
            label="Verge"
            tekst={
              <>
                {formaterFnr(it.vergeEllerFullmektig.motpartsPersonident!)}
                <br />
                {it.vergeEllerFullmektig.omfang &&
                  (omfangMap.get(it.vergeEllerFullmektig.omfang) ?? it.vergeEllerFullmektig.omfang)}
              </>
            }
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
      {isFailureHandler({
        apiResult: soeker,
        errorMessage: 'Kunne ikke hente info om verger',
      })}
    </InfoWrapper>
  )
}
