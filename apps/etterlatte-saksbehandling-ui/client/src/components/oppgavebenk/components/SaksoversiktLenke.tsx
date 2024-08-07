import { NavLink } from 'react-router-dom'

const SaksoversiktLenke = ({ fnr }: { fnr: string }) => (
  <NavLink to="/person" state={{ fnr }}>
    {fnr}
  </NavLink>
)

export default SaksoversiktLenke
