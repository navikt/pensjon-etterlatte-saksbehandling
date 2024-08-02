import { Link } from '@navikt/ds-react'

const SaksoversiktLenke = ({ sakId }: { sakId: string }) => {
  return <Link href={`/sak/${sakId}`}>{sakId}</Link>
}

export default SaksoversiktLenke
