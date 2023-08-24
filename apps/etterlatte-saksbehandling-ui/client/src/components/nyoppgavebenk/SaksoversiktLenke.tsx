import { Link } from '@navikt/ds-react'

const SaksoversiktLenke = ({ fnr }: { fnr: string }) => {
  return <Link href={`person/${fnr}`}>{fnr}</Link>
}

export default SaksoversiktLenke
