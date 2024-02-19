import { EarthIcon } from '@navikt/aksel-icons'
import { Link } from '@navikt/ds-react'
import { useContext, useEffect } from 'react'
import { ConfigContext } from '~clientConfig'
import { SakType } from '~shared/types/sak'
import { getPersongalleriFraSoeknad } from '~shared/api/grunnlag'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapSuccess } from '~shared/api/apiUtils'
import { Persongalleri } from '~shared/types/Person'

interface Props {
  sakId: number
  behandlingId: string
  sakType: SakType
}

export const EessiPensjonLenke = ({ sakId, behandlingId, sakType }: Props) => {
  const configContext = useContext(ConfigContext)

  const [persongalleriStatus, hentPersongalleri] = useApiCall(getPersongalleriFraSoeknad)

  useEffect(() => {
    hentPersongalleri({ behandlingId })
  }, [])

  /**
   * Konvertere til et format EP-Gjenny kan tolke
   **/
  const sakTypeEessiFormat = (sakType: SakType) => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return 'BARNEP'
      case SakType.OMSTILLINGSSTOENAD:
        return 'OMSST'
    }
  }

  const opprettUrl = (persongalleri: Persongalleri) => {
    const params = new URLSearchParams({
      fnr: persongalleri.soeker || '',
      avdodFnr: persongalleri.avdoed?.[0] || '',
      sakType: sakTypeEessiFormat(sakType),
      sakId: sakId.toString(),
    })

    return `${configContext['eessiPensjonUrl']}?${params}`
  }

  return mapSuccess(persongalleriStatus, (persongalleri) => (
    <Link href={opprettUrl(persongalleri.opplysning)} target="_blank">
      <EarthIcon />
    </Link>
  ))
}
