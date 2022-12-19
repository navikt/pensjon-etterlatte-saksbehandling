import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App'
import { Modal } from '@navikt/ds-react'
import { Provider } from 'react-redux'
import { store } from './store/Store'
import { setupWindowOnError } from '~utils/logger'
import ErrorBoundary from '~ErrorBoundary'

Modal.setAppElement?.('#root')
setupWindowOnError()

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <Provider store={store}>
      <ErrorBoundary>
        <App />
      </ErrorBoundary>
    </Provider>
  </React.StrictMode>
)
