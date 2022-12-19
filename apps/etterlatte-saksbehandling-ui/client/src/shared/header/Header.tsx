import { Search } from './Search'
import { Header } from '@navikt/ds-react-internal'
import { useAppSelector } from '~store/Store'
import { Enhet } from './Enhet'

export const HeaderWrapper = () => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  return (
    <Header data-theme={'light'}>
      <Header.Title href={'/'}>Doffen</Header.Title>
      <div style={{ marginLeft: 'auto' }} />
      <Search />
      <Enhet />
      <Header.User data-theme={'dark'} name={user.navn} description={user.ident} />
    </Header>
  )
}
