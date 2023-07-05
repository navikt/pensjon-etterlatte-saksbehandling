import { Link } from '@navikt/ds-react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'

export interface HjemmelLenkeProps {
  tittel: string
  lenke: string
}

export const HjemmelLenke = (props: HjemmelLenkeProps) => {
  return (
    <Link href={props.lenke} target="_blank" rel="noopener noreferrer">
      {props.tittel}
      <ExternalLinkIcon title={props.tittel} />
    </Link>
  )
}
