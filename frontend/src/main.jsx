import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ToastContainer, Bounce } from 'react-toastify'
import App from './App.jsx'
import { AuthProvider } from './state/AuthContext.jsx'
import { ParametersProvider } from './state/ParametersContext.jsx'
import './styles.css'
import 'react-toastify/dist/ReactToastify.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ParametersProvider>
          <App />
          <ToastContainer position="top-right" autoClose={2000} theme="light" transition={Bounce} hideProgressBar />
        </ParametersProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
)
