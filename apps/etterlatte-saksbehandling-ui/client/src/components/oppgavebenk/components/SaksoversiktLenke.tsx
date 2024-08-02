import { Link } from '@navikt/ds-react'

const SaksoversiktLenke = ({ sakId }: { sakId: number }) => {
  return <Link href={`/sak/${sakId}`}>{sakId}</Link>
}

export default SaksoversiktLenke
