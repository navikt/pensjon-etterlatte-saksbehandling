import { GlobeIcon } from '@navikt/aksel-icons'
import { Link } from '@navikt/ds-react'
import { useContext } from 'react'
import { ConfigContext } from '~clientConfig'

export const EessiPensjonLenke = () => {
  const configContext = useContext(ConfigContext)

  /**
   * TODO:
   *    Må sende med diverse params når EESSI Pensjon har laget støtte for å motta
   *    - aktoerId
   *    - sakId
   *    - kravId
   *    - vedtakId
   *    - sakType
   *    - avdodFnr
   **/
  return (
    <Link href={configContext['eessiPensjonUrl']} target="_blank">
      <GlobeIcon />
    </Link>
  )
}
