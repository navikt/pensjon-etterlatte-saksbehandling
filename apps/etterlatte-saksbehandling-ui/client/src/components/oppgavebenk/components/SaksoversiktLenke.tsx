import { Link } from '@navikt/ds-react'
import Kryptering2 from "~shared/api/krypter";

const SaksoversiktLenke = ({ fnr }: { fnr: string }) => {
    return <Link href={`person/${Kryptering2({fnr})}`}>{fnr}</Link>
}

export default SaksoversiktLenke
