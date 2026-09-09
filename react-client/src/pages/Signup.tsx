import { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { signup } from "../api/authApi";
function Signup() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [fullName, setFullName] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const handleSignup = async () => {
        try {
            const res = await signup(email, fullName, password);
            alert(res);
            navigate("/login")
        } catch (err: unknown) {
            if (axios.isAxiosError<{ message?: string }>(err) && err.response) {
                setError(err.response.data?.message ?? "Đăng ký thất bại");
                return;
            }
            setError("Không thể kết nối tới server");
        }
    }
    return (
        <>
            <h1>Trang Dang Ky</h1>
            <input type="Email" value={email} placeholder="Input Email" onChange={(e) => setEmail(e.target.value)}></input><br />
            <input type="text" value={fullName} placeholder="Input Fullname" onChange={(e) => setFullName(e.target.value)}></input><br />
            <input type="password" value={password} placeholder="Input Password" onChange={(e) => setPassword(e.target.value)}></input><br />
            <button onClick={handleSignup}>Signup</button>
            <button onClick={() => navigate("/login")}>Ve Login</button><br />
            {error && <span style={{ color: "red" }} >{error}</span >}
        </>
    )
}
export default Signup;
