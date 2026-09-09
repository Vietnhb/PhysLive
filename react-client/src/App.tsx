import { Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Admin from "./pages/Admin";
import { useEffect } from "react";
import { getToken, clearToken } from "./utils/token";
import { isTokenExpired } from "./utils/jwt";

function App() {

  useEffect(() => {
    const token = getToken();
    if (token && isTokenExpired(token)) {
      clearToken();
    }
  }, [])

  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/admin" element={<Admin />} />
    </Routes>
  )
}
export default App;