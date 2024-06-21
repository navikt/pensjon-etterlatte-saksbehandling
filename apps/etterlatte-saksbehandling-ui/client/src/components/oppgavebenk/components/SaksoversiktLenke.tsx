import { Link } from '@navikt/ds-react'
import krypterFnr from '~shared/api/krypter'

const SaksoversiktLenke = ({ fnr }: { fnr: string }) => {
  return <Link href={`person/${krypterFnr({ fnr })}`}>{fnr}</Link>
}

export default SaksoversiktLenke
