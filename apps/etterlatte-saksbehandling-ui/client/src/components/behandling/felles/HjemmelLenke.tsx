import { Link } from '@navikt/ds-react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { Hjemmel } from '~components/behandling/virkningstidspunkt/utils'

export const HjemmelLenke = (props: Hjemmel) => {
  return (
    <Link href={props.lenke} target="_blank" rel="noopener noreferrer">
      {props.tittel}
      <ExternalLinkIcon title={props.tittel} />
    </Link>
  )
}
