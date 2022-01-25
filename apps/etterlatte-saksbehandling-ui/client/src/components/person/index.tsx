import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom';
import { getPerson, opprettBehandlingPaaSak, opprettSakPaaPerson } from '../../shared/api/person'

export const Person = () => {
  const [personData, setPersonData] = useState({})
  const match = useParams<{fnr: string}>();

  const sakIdInput = useRef() as React.MutableRefObject<HTMLInputElement>;
  
  console.log(personData)
  useEffect(() => {
    (async () => {
        if(match.fnr) {
          const person = await getPerson(match.fnr);
          setPersonData(person);
        }
    })();
  }, [])

  const opprettSak = () => {
    if(match.fnr) {
      opprettSakPaaPerson(match.fnr)
    }
  }

  const opprettBehandling = () => {
    if(sakIdInput.current.value) {
      console.log(sakIdInput.current.value)
      opprettBehandlingPaaSak(Number(sakIdInput.current.value))
    }
  }

  return <div>Personinfo
    <p>
    <button onClick={opprettSak}>Opprett/hent sak</button>
    </p>

    <p>
    <input ref={sakIdInput} placeholder="sakid" name="sakid" />
    <button onClick={opprettBehandling}>Opprett behandling på denne saken</button>
    </p>
  </div>
}
