import { Search } from './Search'
import { InternalHeader } from '@navikt/ds-react'
import { useAppSelector } from '~store/Store'

export const HeaderBanner = () => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  return (
    <InternalHeader>
      <InternalHeader.Title href={'/'} as="h1">
        Gjenny{' '}
      </InternalHeader.Title>
      <Search />
      <InternalHeader.User name={user.navn} description={user.ident} />
    </InternalHeader>
  )
}
