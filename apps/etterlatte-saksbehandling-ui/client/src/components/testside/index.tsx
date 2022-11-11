import { useState } from 'react'
import Spinner from '~shared/Spinner'
import { Container } from '~shared/styled'

export const Testside = () => {
  const [input, setInput] = useState<string>('')
  const [loadingDelete, setLoadingDelete] = useState<boolean>(false)

  const onChange = (e: any) => {
    setInput(e.target.value)
  }

  const submitDelete = async () => {
    setLoadingDelete(true)
    try {
      const result = await fetch(`${process.env.REACT_APP_VEDTAK_URL}/api/saker/${input}/behandlinger`, {
        method: 'delete',
      })
      console.log(result)
      setLoadingDelete(false)
    } catch (e) {
      console.log(e)
      setLoadingDelete(false)
    }
  }

  return (
    <Container>
      <h1>Testside</h1>
      <p>Funksjonalitet som kun er ment for testmiljøet</p>

      <h2>Slett alle behandlinger på sak</h2>

      {loadingDelete ? (
        <Spinner visible={loadingDelete} label="Sletter behandlinger på sak" />
      ) : (
        <div>
          <input type="tel" value={input} onChange={onChange} placeholder="sakId" />
          <button onClick={submitDelete}>Slett</button>
        </div>
      )}
    </Container>
  )
}
