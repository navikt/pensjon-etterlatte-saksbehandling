import { Link } from '@navikt/ds-react'

const SaksoversiktLenke = ({ sakId }: { sakId: number }) => {
  return <Link href={`/person/${sakId}`}>{sakId}</Link>
}

export default SaksoversiktLenke
