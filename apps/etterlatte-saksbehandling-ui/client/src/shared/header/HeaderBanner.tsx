import { Search } from './Search'
import { InternalHeader } from '@navikt/ds-react'
import { useAppSelector } from '~store/Store'

export const HeaderBanner = () => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  return (
    <InternalHeader data-theme="light">
      <InternalHeader.Title href="/">Gjenny</InternalHeader.Title>
      <div style={{ marginLeft: 'auto' }} />
      <Search />
      <InternalHeader.User data-theme="dark" name={user.navn} description={user.ident} />
    </InternalHeader>
  )
}
