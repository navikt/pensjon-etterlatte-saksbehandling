import { Link } from '@navikt/ds-react'

const SaksoversiktLenke: React.FC<{ fnr: string }> = ({ fnr }) => {
  return <Link href={`person/${fnr}`}>{fnr}</Link>
}

export default SaksoversiktLenke
