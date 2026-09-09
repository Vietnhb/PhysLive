import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { login } from "../api/authApi";
import { setToken } from "../utils/token";
function Login() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const handleLogin = async () => {
        try {
            const res = await login(email, password);
            setToken(res.token);
            console.log(res);
            navigate("/")
        } catch (err: any) {
            // Check lỗi kết nối trước
            if (!err.response) {
                setError("Không thể kết nối tới server");
                return;
            }
            // Check lỗi 401
            if (err.response.status === 401) {
                setError(err.response.data.message);
                return;
            }
            // Lỗi khác từ server
            setError("Có lỗi xảy ra từ server");
        }

    }
    return (
        <>
            <div>Trang Dang Nhap</div>
            <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} /><br />
            <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)}></input><br />
            <button onClick={handleLogin}>Login</button>
            <button onClick={() => navigate("/")} >Ve Trang chu</button>
            <button onClick={() => navigate("/signup")}>Dang Ky</button>
            <br />
            {error && <p style={{ color: "red" }}>{error}</p>}
        </>
    )
}
export default Login;