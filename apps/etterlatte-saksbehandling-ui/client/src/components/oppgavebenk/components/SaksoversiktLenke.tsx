import { Link } from '@navikt/ds-react'

const SaksoversiktLenke = ({ sakId }: { sakId: string }) => {
  return <Link href={`person/${sakId}`}>{sakId}</Link>
}

export default SaksoversiktLenke
