import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App'
import { Modal } from '@navikt/ds-react'
import { Provider } from 'react-redux'
import { store } from './store/Store'

Modal.setAppElement?.('#root')

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>
)
