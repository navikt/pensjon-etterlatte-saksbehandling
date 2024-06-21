import { Link } from '@navikt/ds-react'
import Kryptering from '~shared/api/krypter'

const SaksoversiktLenke = ({ fnr }: { fnr: string }) => {
  return <Link href={`person/${Kryptering({ fnr })}`}>{fnr}</Link>
}

export default SaksoversiktLenke
