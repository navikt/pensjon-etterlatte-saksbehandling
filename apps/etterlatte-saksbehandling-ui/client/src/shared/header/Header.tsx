import { Search } from './Search'
import { Header } from '@navikt/ds-react-internal'
import { useAppSelector } from '~store/Store'

export const HeaderWrapper = () => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  return (
    <Header data-theme={'light'}>
      <Header.Title href={'/'}>Gjenny</Header.Title>
      <div style={{ marginLeft: 'auto' }} />
      <Search />
      <Header.User data-theme={'dark'} name={user.navn} description={user.ident} />
    </Header>
  )
}
