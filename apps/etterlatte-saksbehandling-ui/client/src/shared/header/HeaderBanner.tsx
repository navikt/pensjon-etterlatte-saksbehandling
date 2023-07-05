import { Search } from './Search'
import { InternalHeader } from '@navikt/ds-react'
import { useAppSelector } from '~store/Store'

export const HeaderBanner = () => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  return (
    <InternalHeader>
      <InternalHeader.Title data-theme={'light'} href={'/'}>
        Gjenny
      </InternalHeader.Title>
      <div style={{ marginLeft: 'auto' }} />
      <Search />
      <InternalHeader.User name={user.navn} description={user.ident} />
    </InternalHeader>
  )
}
