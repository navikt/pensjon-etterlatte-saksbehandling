import { Link } from "@navikt/ds-react";


const BrukeroversiktLenke: React.FC<{fnr: string}> = ({fnr}) => {
  return (
    <Link href={`person/${fnr}`}>
      {fnr}
    </Link>
  )
}

export default BrukeroversiktLenke;
