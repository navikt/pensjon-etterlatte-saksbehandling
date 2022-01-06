import { useEffect, useState } from 'react'
import styled from 'styled-components'
import { getZUser } from '../api/user'

export const Bruker = () => {
  const [user, setUser] = useState<any>()

  useEffect(() => {
    (async () => {
      const user: any = await getZUser()
      console.log(user)
      setUser(user)
    })()
  }, [])

  return (
    <BrukerWrap>
      {user?.data?.fornavn} {user?.data?.etternavn} <Ident>({user?.data?.ident})</Ident>
    </BrukerWrap>
  )
}

const BrukerWrap = styled.div`
  padding: 1em;
`;

const Ident = styled.span`
  font-size: 0.8em;
`;