import { ReactNode, useEffect } from 'react'
import ReactDOM from 'react-dom'

export const Portal = (props: { children: ReactNode }) => {
  const div = document.createElement('div')
  div.id = 'portal'

  useEffect(() => {
    document.body.appendChild(div)
    return () => {
      document.body.removeChild(div)
    }
  }, [div])

  return ReactDOM.createPortal(<div>{props.children}</div>, div)
}
