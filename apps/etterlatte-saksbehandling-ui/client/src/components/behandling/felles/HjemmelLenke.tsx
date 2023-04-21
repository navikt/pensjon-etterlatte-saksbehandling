import { Link } from '@navikt/ds-react'
import { ExternalLink } from '@navikt/ds-icons'

export interface HjemmelLenkeProps {
  tittel: string
  lenke: string
}

export const HjemmelLenke = (props: HjemmelLenkeProps) => {
  return (
    <Link href={props.lenke} target="_blank" rel="noopener noreferrer">
      {props.tittel}
      <ExternalLink title={props.tittel} />
    </Link>
  )
}
